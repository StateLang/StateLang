package smc.semanticAnalyzer;

import smc.Utilities;

import java.util.*;

public class InheritanceTree implements Iterable<InheritanceTree.InheritanceNode> {
    record InheritanceWarnings(InheritanceWarningType type, String info) {}

    enum InheritanceWarningType {
        REDUNDANT_SUPER_STATE,
        IMPLICIT_SUPER_STATE
    }

    static class InheritanceNode {
        String name;
        LinkedHashMap<String, InheritanceNode> children = new LinkedHashMap<>();

        InheritanceNode(String name) {
            this.name = name;
        }

        InheritanceNode(String name, LinkedHashMap<String, InheritanceNode> children) {
            this.name = name;
            this.children = children;
        }
    }

    private final InheritanceNode root;
    private final Map<String, InheritanceWarnings> warnings;
    private Map<String, List<Deque<InheritanceNode>>> leafPaths;

    public InheritanceTree(Map<String, List<String>> parentChildren, Map<String, Boolean> isRootSuperState) {
        root = new InheritanceNode("__root__");
        warnings = new HashMap<>(parentChildren.size());
        for (Map.Entry<String, List<String>> ent : parentChildren.entrySet()) {
            root.children.put(ent.getKey(), new InheritanceNode(ent.getKey()));
            for (String nodeName : ent.getValue()) {
                root.children.get(ent.getKey()).children.put(nodeName, new InheritanceNode(nodeName));
            }
        }
        Utilities.sortMapByValue(root.children, Comparator.comparingInt(node -> node.children.size()));
        List<Map.Entry<String, InheritanceNode>> list =  new ArrayList<>(root.children.entrySet());
        for (int i = 0; i < list.size(); ++i) {
            InheritanceNode src = list.get(i).getValue();
            for (int j = i + 1; j < list.size(); ++j) {
                InheritanceNode dst = list.get(j).getValue();
                if (warnings.get(dst.name) == null && addIfSubset(src, dst)) {
                    warnings.put(src.name, new InheritanceWarnings(
                                                isRootSuperState.get(src.name) ?
                                                InheritanceWarningType.IMPLICIT_SUPER_STATE :
                                                InheritanceWarningType.REDUNDANT_SUPER_STATE,
                                                dst.name
                                           )
                    );
                    root.children.remove(src.name);
                    break;
                }
            }
        }
        constructIntersectionStatesMap();
    }

    public Map<String, List<Deque<InheritanceNode>>> getIntersections() {
        return leafPaths;
    }

    public Map<String, InheritanceWarnings> getWarnings() {
        return warnings;
    }

    private static class InheritanceNodeIterator implements Iterator<InheritanceNode> {

        private final Stack<InheritanceNode> stack;

        InheritanceNodeIterator(InheritanceNode root) {
            stack = new Stack<>();
            if (root != null)
                stack.push(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public InheritanceNode next() {
            if (!hasNext())
                throw new NoSuchElementException("No more elements in the tree");

            InheritanceNode current = stack.pop();
            if (current != null && !current.children.isEmpty())
                for (InheritanceNode child : current.children.values())
                    if (child != null)
                        stack.push(child);

            return current;
        }
    }
    @Override
    public Iterator<InheritanceNode> iterator() {
        return new InheritanceNodeIterator(root);
    }

    private static boolean addIfSubset(InheritanceNode src, InheritanceNode dst) {
        if (addParent(dst, src)) {
            removeNestedDuplicates(dst);
            return true;
        } else if (unionIfFound(src, dst)) {
            removeNestedDuplicates(dst);
            return true;
        }

        return false;
    }

    private static boolean addParent(InheritanceNode target, InheritanceNode suspectedParent) {
        if (target == null)
            return false;
        LinkedHashMap<String, InheritanceNode> found = new LinkedHashMap<>(suspectedParent.children.size());
        for (InheritanceNode n : suspectedParent.children.values()) {
            InheritanceNode f = target.children.get(n.name);
            if (f == null)
                break;
            found.put(n.name, f);
        }

        if (found.size() > 0 && found.size() == suspectedParent.children.size()) {
            target.children.put(suspectedParent.name,
                    union(suspectedParent, new InheritanceNode(suspectedParent.name, found)));
            for (InheritanceNode n : found.values())
                target.children.remove(n.name);
            return true;
        }

        for (InheritanceNode child : target.children.values())
            if (addParent(child, suspectedParent))
                return true;
        return false;
    }

    private static void removeNestedDuplicates(InheritanceNode root) {
        if (root == null || root.children.isEmpty())
            return;
        Utilities.sortMapByValue(root.children, Comparator.comparingInt(node -> node.children.size()));
        List<Map.Entry<String, InheritanceNode>> sortedNodeEntries = new ArrayList<>(root.children.entrySet());

        for (int i = 0; i < sortedNodeEntries.size(); ++i) {
            InheritanceNode deleteIfFound = sortedNodeEntries.get(i).getValue();
            for (int j = i + 1; j < sortedNodeEntries.size(); ++j)
                if (unionIfFound(deleteIfFound, sortedNodeEntries.get(j).getValue()))
                    root.children.remove(deleteIfFound.name);
        }
    }

    private static boolean unionIfFound(InheritanceNode needleAndSrc, InheritanceNode haystackAndDst) {
        if (haystackAndDst == null || haystackAndDst.children.isEmpty())
            return false;

        InheritanceNode suspectedDst = haystackAndDst.children.get(needleAndSrc.name);
        if (suspectedDst != null) {
            union(needleAndSrc, suspectedDst);
            return true;
        }

        for (InheritanceNode newHaystackAndDst : haystackAndDst.children.values())
            if(unionIfFound(needleAndSrc, newHaystackAndDst))
                return true;

        return false;
    }

    private static InheritanceNode union(InheritanceNode src, InheritanceNode dst) {
        for (InheritanceNode srcNode : src.children.values()) {
            InheritanceNode dstNode = dst.children.get(srcNode.name);
            dst.children.put(srcNode.name, (dstNode == null) ? srcNode : union(srcNode, dstNode));
        }
        return dst;
    }

    private void constructIntersectionStatesMap() {
        leafPaths = new HashMap<>();
        Deque<InheritanceNode> path = new ArrayDeque<>();
        constructLeafPathMap(root, path);

        leafPaths.entrySet().removeIf(entry -> entry.getValue().size() < 2);
    }

    private void constructLeafPathMap(InheritanceNode node, Deque<InheritanceNode> path) {
        if (node == null)
            return;

        path.addLast(node);

        if (node.children.isEmpty()) {
            List<Deque<InheritanceNode>> paths = leafPaths.get(node.name);
            if (paths == null)
                paths = new ArrayList<>();
            paths.add(new ArrayDeque<>(path));
            leafPaths.put(node.name, paths);
        }

        for (InheritanceNode child : node.children.values())
            constructLeafPathMap(child, path);

        path.removeLast();
    }

    public static void main(String[] args) {
        Map<String, List<String>> parentChildren = Map.ofEntries(
                Map.entry("c", List.of("m", "n", "a", "x", "b", "1", "i", "r", "g")),
                Map.entry("y", List.of("g", "w", "2", "k")),
                Map.entry("j", List.of("3", "1", "x")),
                Map.entry("v", List.of("l", "z", "k")),
                Map.entry("x", List.of("3", "1", "q")),
                Map.entry("s", List.of("r", "i")),
                Map.entry("1", List.of("3", "2")),
                Map.entry("m", List.of("n", "a")),
                Map.entry("u", List.of("k"))
                );
        Map<String, Boolean> isRootSuperState = Map.ofEntries(
                Map.entry("c", true),
                Map.entry("y", true),
                Map.entry("j", true),
                Map.entry("v", true),
                Map.entry("x", false),
                Map.entry("s", true),
                Map.entry("1", false),
                Map.entry("m", false),
                Map.entry("u", true)
        );
        InheritanceTree it = new InheritanceTree(parentChildren, isRootSuperState);
        var s = it.getWarnings();
        for (var e : it) {
            System.out.println(e.name);
        }
    }
}
