package smc;

import java.util.*;

import static java.util.Collections.swap;

public class Utilities {
  public static String commaList(List<String> names) {
    StringBuilder commaList = new StringBuilder();
    boolean first = true;
    for (String name : names) {
      commaList.append(first ? "" : ",").append(name);
      first = false;
    }
    return commaList.toString();
  }

  public static List<String> addPrefix(String prefix, List<String> list) {
    List<String> result = new ArrayList<>();
    for (String element : list)
      result.add(prefix + element);
    return result;
  }

  public static String compressWhiteSpace(String s) {
    return s.replaceAll("\\n+", "\n")
            .replaceAll("[\t ]+", " ")
            .replaceAll(" *\n *", "\n");
  }

  public static <K, V> void sortMapByValue(LinkedHashMap<K, V> map, final Comparator<? super V> c) {
    List<Map.Entry<K, V>> entries = new ArrayList<>(map.entrySet());

    insertionSort(entries, (lhs, rhs) -> c.compare(lhs.getValue(), rhs.getValue()));

    map.clear();
    for(Map.Entry<K, V> e : entries)
      map.put(e.getKey(), e.getValue());
  }
  public static <T> void insertionSort(List<T> a, Comparator<T> comparator) {
    int n = a.size();
    for (int i = 1; i < n; ++i)
      for (int j = i; j > 0 && comparator.compare(a.get(j), a.get(j-1)) < 0; --j)
        swap(a, j, j-1);
  }
}
