package smc.semanticAnalyzer;

import smc.Utilities;

import java.util.*;
import java.util.stream.Collectors;

import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError.ID.*;

public class InheritanceTreeSimplifier {
  private static class InheritanceTree implements Iterable<InheritanceTree.InheritanceNode> {
    static class InheritanceNode {
      final String name;
      final LinkedHashMap<String, InheritanceNode> children;

      InheritanceNode(String name) {
        this.name = name;
        this.children = new LinkedHashMap<>();
      }

      InheritanceNode(String name, LinkedHashMap<String, InheritanceNode> children) {
        this.name = name;
        this.children = children;
      }

    }

    private final InheritanceNode root;

    public InheritanceTree(Map<String, List<String>> parentChildren) {
      root = new InheritanceNode("__root__");
      for (Map.Entry<String, List<String>> ent : parentChildren.entrySet()) {
        root.children.put(ent.getKey(), new InheritanceNode(ent.getKey()));
        for (String nodeName : ent.getValue())
          root.children.get(ent.getKey()).children.put(nodeName, new InheritanceNode(nodeName));
      }
    }

    private static class InheritanceNodeIterator implements Iterator<InheritanceNode> {
      private final Stack<InheritanceNode> stack;

      InheritanceNodeIterator(InheritanceNode root) {
        stack = new Stack<>();
        if (root != null)
          for (InheritanceNode child : root.children.values())
            if (child != null)
              stack.push(child);
      }

      @Override
      public InheritanceNode next() {
        if (!hasNext())
          throw new NoSuchElementException("No more elements in the tree");

        final InheritanceNode current = stack.pop();
        if (current != null && !current.children.isEmpty())
          for (InheritanceNode child : current.children.values())
            if (child != null)
              stack.push(child);

        return current;
      }

      @Override
      public boolean hasNext() {
        return !stack.isEmpty();
      }
    }

    @Override
    public Iterator<InheritanceNode> iterator() {
      return new InheritanceNodeIterator(root);
    }

    private static boolean substituteIfSubset(InheritanceNode src, InheritanceNode dst) {
      if (substituteSubStatesWithSuperStateIfFound(dst, src) || unionIfFound(src, dst)) {
        removeNestedDuplicates(dst);
        return true;
      }

      return false;
    }

    private static boolean substituteSubStatesWithSuperStateIfFound(InheritanceNode target, InheritanceNode suspectedSuperState) {
      if (target == null)
        return false;

      LinkedHashMap<String, InheritanceNode> found = new LinkedHashMap<>(suspectedSuperState.children.size());
      for (InheritanceNode n : suspectedSuperState.children.values()) {
        InheritanceNode f = target.children.get(n.name);
        if (f == null)
          break;
        found.put(n.name, f);
      }

      if (found.size() > 0 && found.size() == suspectedSuperState.children.size()) {
        target.children.put(suspectedSuperState.name,
            union(suspectedSuperState, new InheritanceNode(suspectedSuperState.name, found)));
        for (InheritanceNode n : found.values())
          target.children.remove(n.name);
        return true;
      }

      for (InheritanceNode child : target.children.values())
        if (substituteSubStatesWithSuperStateIfFound(child, suspectedSuperState))
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
        if (unionIfFound(needleAndSrc, newHaystackAndDst))
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
  }

  private Map<String, List<Deque<InheritanceTree.InheritanceNode>>> intersectionHierarchies;
  private final InheritanceTree tree;
  private final SemanticStateMachine ast;

  public InheritanceTreeSimplifier(SemanticStateMachine ast) {
    this.ast = ast;
    tree = new InheritanceTree(makeParentChildrenMap());
    simplifyInheritanceTree();
  }

  private void simplifyInheritanceTree() {
    Map<String, Boolean> isRootSuperState = makeIsRootSuperStateMap();
    Utilities.sortMapByValue(tree.root.children, Comparator.comparingInt(node -> node.children.size()));
    List<Map.Entry<String, InheritanceTree.InheritanceNode>> rootSuperStateslist = new ArrayList<>(tree.root.children.size());
    List<Map.Entry<String, InheritanceTree.InheritanceNode>> restStateslist = new ArrayList<>(tree.root.children.size());
    for (Map.Entry<String, InheritanceTree.InheritanceNode> entry : tree.root.children.entrySet()) {
      if (isRootSuperState.get(entry.getKey()))
        rootSuperStateslist.add(entry);
      else
        restStateslist.add(entry);
    }
    Set<String> removed = new HashSet<>();
    substituteAll(rootSuperStateslist, removed, isRootSuperState);
    restStateslist.addAll(rootSuperStateslist);
    substituteAll(restStateslist, removed, isRootSuperState);

    handleIntersectionStates();
    dfsSortStatesInAST();
  }

  private void handleIntersectionStates() {
    constructIntersectionStatesMap();
    warnAndEliminateIntersections();
    addSimplifiedSuperStateIfAnyForEachState();
  }

  public SemanticStateMachine simplify() {
    return ast;
  }

  private Map<String, List<String>> makeParentChildrenMap() {
    Map<String, List<String>> parentChildrenMap = new HashMap<>();
    for (SemanticStateMachine.SemanticState state : ast.states.values()) {
      if (!parentChildrenMap.containsKey(state.name))
        parentChildrenMap.put(state.name, new ArrayList<>());
      for (SemanticStateMachine.SemanticState superState : state.superStates) {
        if (!parentChildrenMap.containsKey(superState.name))
          parentChildrenMap.put(superState.name, new ArrayList<>());
        parentChildrenMap.get(superState.name).add(state.name);
      }
    }
    return parentChildrenMap;
  }

  private Map<String, Boolean> makeIsRootSuperStateMap() {
    Map<String, Boolean> isRootSuperState = new HashMap<>(tree.root.children.size());
    for (SemanticStateMachine.SemanticState state : ast.states.values())
      isRootSuperState.put(state.name, state.superStates.isEmpty());
    return isRootSuperState;
  }

  private void substituteAll(List<Map.Entry<String, InheritanceTree.InheritanceNode>> list, Set<String> removed, Map<String, Boolean> isRootSuperState) {
    for (int i = 0; i < list.size(); ++i) {
      InheritanceTree.InheritanceNode src = list.get(i).getValue();
      if (removed.contains(src.name))
        continue;
      for (int j = i + 1; j < list.size(); ++j) {
        InheritanceTree.InheritanceNode dst = list.get(j).getValue();
        if (!removed.contains(dst.name) && InheritanceTree.substituteIfSubset(src, dst)) {
          if (!src.children.isEmpty())
            ast.warnings.add(
                isRootSuperState.get(src.name) ? new SemanticStateMachine.AnalysisError(
                    IMPLICIT_SUPER_STATE,
                    src.name + ":" + dst.name
                ) : new SemanticStateMachine.AnalysisError(
                    REDUNDANT_SUPER_STATE,
                    src.children
                        .values().stream()
                        .map(inheritanceNode -> inheritanceNode.name)
                        .collect(Collectors.joining(","))
                        + ":"
                        + dst.name
                )
            );
          removed.add(src.name);
          tree.root.children.remove(src.name);
          break;
        }
      }
    }
  }

  private void constructIntersectionStatesMap() {
    intersectionHierarchies = new HashMap<>();
    Deque<InheritanceTree.InheritanceNode> path = new ArrayDeque<>();
    constructLeafPathMap(tree.root, path);

    intersectionHierarchies.entrySet().removeIf(entry -> entry.getValue().size() < 2);
  }

  private void constructLeafPathMap(InheritanceTree.InheritanceNode node, Deque<InheritanceTree.InheritanceNode> path) {
    if (node == null)
      return;

    path.addLast(node);

    if (node.children.isEmpty()) {
      List<Deque<InheritanceTree.InheritanceNode>> paths = intersectionHierarchies.get(node.name);
      if (paths == null)
        paths = new ArrayList<>();
      Deque<InheritanceTree.InheritanceNode> pushedPath = new ArrayDeque<>(path);
      pushedPath.removeFirst();
      if (!pushedPath.isEmpty())
        pushedPath.removeLast();
      paths.add(pushedPath);
      intersectionHierarchies.put(node.name, paths);
    }

    for (InheritanceTree.InheritanceNode child : node.children.values())
      constructLeafPathMap(child, path);

    path.removeLast();
  }

  private void warnAndEliminateIntersections() {
    addAllSuperStateTransitions();
    for (SemanticStateMachine.SemanticState state : ast.states.values()) {
      if (intersectionHierarchies.containsKey(state.name)) {
        ast.warnings.add(new SemanticStateMachine.AnalysisError(
            SUPER_STATES_INTERSECTION, state.name
        ));
        for (SemanticStateMachine.SemanticTransition transition : state.transitions)
          addSuperExitActions(state, transition);
      } else {
        for (SemanticStateMachine.SemanticTransition transition : state.transitions)
          if (intersectionHierarchies.containsKey(transition.nextState.name))
            addSuperEntryActions(state, transition);
      }
    }
    removeIntersectionStatesFromInheritanceTreeAndAddThemToTheRoot();
  }

  private void addAllSuperStateTransitions() {
    for (Map.Entry<String, List<Deque<InheritanceTree.InheritanceNode>>> paths : intersectionHierarchies.entrySet()) {
      Set<InheritanceTree.InheritanceNode> superStates = new LinkedHashSet<>();
      for (Deque<InheritanceTree.InheritanceNode> path : paths.getValue())
        superStates.addAll(path);

      for (InheritanceTree.InheritanceNode superState : superStates) {
        List<SemanticStateMachine.SemanticTransition> transitions = new ArrayList<>();
        for (SemanticStateMachine.SemanticTransition t : ast.states.get(superState.name).transitions)
          transitions.add(new SemanticStateMachine.SemanticTransition(t));
        ast.states.get(paths.getKey()).transitions.addAll(transitions);
      }
    }
  }

  private void addSuperExitActions(SemanticStateMachine.SemanticState state, SemanticStateMachine.SemanticTransition transition) {
    Set<InheritanceTree.InheritanceNode> superStates = new LinkedHashSet<>();
    for (Deque<InheritanceTree.InheritanceNode> path : intersectionHierarchies.get(state.name))
      superStates.addAll(path);

    for (InheritanceTree.InheritanceNode superState : superStates)
      if (!isSuperStateOf(superState, transition.nextState))
        transition.actions.addAll(0, ast.states.get(superState.name).exitActions);
  }

  private void addSuperEntryActions(SemanticStateMachine.SemanticState state, SemanticStateMachine.SemanticTransition transition) {
    Set<InheritanceTree.InheritanceNode> superStates = new LinkedHashSet<>();
    for (Deque<InheritanceTree.InheritanceNode> path : intersectionHierarchies.get(transition.nextState.name))
      superStates.addAll(path);
    List<InheritanceTree.InheritanceNode> superStatesList = new ArrayList<>(superStates);
    Collections.reverse(superStatesList);
    for (InheritanceTree.InheritanceNode superState : superStatesList)
      if (!isSuperStateOf(superState, state))
        transition.actions.addAll(0, ast.states.get(superState.name).entryActions);
  }

  private boolean isSuperStateOf(InheritanceTree.InheritanceNode superState, SemanticStateMachine.SemanticState state) {
    LinkedHashSet<String> hierarchy = new LinkedHashSet<>();
    addAllStatesInHierarchyLeafFirst(state, hierarchy);
    return hierarchy.contains(superState.name);
  }

  private void addAllStatesInHierarchyLeafFirst(SemanticStateMachine.SemanticState state, LinkedHashSet<String> hierarchy) {
    for (SemanticStateMachine.SemanticState superState : state.superStates)
      addAllStatesInHierarchyLeafFirst(superState, hierarchy);
    hierarchy.add(state.name);
  }

  private void removeIntersectionStatesFromInheritanceTreeAndAddThemToTheRoot() {
    for (Map.Entry<String, List<Deque<InheritanceTree.InheritanceNode>>> paths : intersectionHierarchies.entrySet()) {
      for (Deque<InheritanceTree.InheritanceNode> path : paths.getValue()) {
        path.addFirst(tree.root);
        path.addLast(path.getLast().children.get(paths.getKey()));
        Iterator<InheritanceTree.InheritanceNode> it = path.descendingIterator();
        InheritanceTree.InheritanceNode state = it.next();
        tree.root.children.put(state.name, state);
        do {
          if (state.children.size() == 0) {
            String stateName = state.name;
            state = it.next();
            state.children.remove(stateName);
          } else {
            break;
          }
        } while (it.hasNext());
        path.removeFirst();
        if (!path.isEmpty())
          path.removeLast();
        ast.states.get(paths.getKey()).superStates.clear();
      }
    }
  }

  private void addSimplifiedSuperStateIfAnyForEachState() {
    for (InheritanceTree.InheritanceNode node : tree) {
      for (InheritanceTree.InheritanceNode child : node.children.values()) {
        SemanticStateMachine.SemanticState state = ast.states.get(child.name);
        state.superStates.clear();
        state.superStates.add(ast.states.get(node.name));
      }
    }
  }

  private void dfsSortStatesInAST() {
    LinkedHashMap<String, SemanticStateMachine.SemanticState> states = new LinkedHashMap<>();
    for (InheritanceTree.InheritanceNode node : tree)
      states.put(node.name, ast.states.get(node.name));
    ast.states = states;
  }
}
