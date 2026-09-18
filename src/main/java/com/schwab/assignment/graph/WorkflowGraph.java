package com.schwab.assignment.graph;

import java.util.*;

/** Validated dependency graph shared by scheduling and selective replanning. */
public final class WorkflowGraph {
  private WorkflowGraph() {}
  public static Map<String,Stage> of(Stage... stages){
    Map<String,Stage> graph=new LinkedHashMap<>();
    for(Stage stage:stages)if(graph.putIfAbsent(stage.id(),stage)!=null)throw new IllegalArgumentException("duplicate stage: "+stage.id());
    graph.values().forEach(stage->stage.dependsOn().forEach(dependency->{if(!graph.containsKey(dependency))throw new IllegalArgumentException("stage "+stage.id()+" depends on unknown stage: "+dependency);}));
    detectCycles(graph); return Collections.unmodifiableMap(graph);
  }
  public static Set<String> downstreamOf(Map<String,Stage> graph,String stageId){
    if(!graph.containsKey(stageId))throw new IllegalArgumentException("unknown stage: "+stageId);
    Set<String> result=new LinkedHashSet<>(); Deque<String> queue=new ArrayDeque<>(); queue.add(stageId);
    while(!queue.isEmpty()){String current=queue.remove();if(!result.add(current))continue;graph.values().stream().filter(stage->stage.dependsOn().contains(current)).map(Stage::id).sorted().forEach(queue::add);}
    return result;
  }
  public static List<List<String>> executionLayers(Map<String,Stage> graph){
    Map<String,Integer> depth=new LinkedHashMap<>();
    for(String id:topologicalOrder(graph)){Set<String> dependencies=graph.get(id).dependsOn();depth.put(id,dependencies.isEmpty()?0:dependencies.stream().mapToInt(depth::get).max().orElse(0)+1);}
    Map<Integer,List<String>> layers=new TreeMap<>(); depth.forEach((id,value)->layers.computeIfAbsent(value,ignored->new ArrayList<>()).add(id)); layers.values().forEach(Collections::sort); return List.copyOf(layers.values());
  }
  private static List<String> topologicalOrder(Map<String,Stage> graph){
    Map<String,Integer> remaining=new LinkedHashMap<>();graph.forEach((id,stage)->remaining.put(id,stage.dependsOn().size()));
    PriorityQueue<String> ready=new PriorityQueue<>();remaining.forEach((id,count)->{if(count==0)ready.add(id);});List<String> order=new ArrayList<>();
    while(!ready.isEmpty()){String current=ready.remove();order.add(current);graph.values().stream().filter(stage->stage.dependsOn().contains(current)).map(Stage::id).sorted().forEach(dependent->{int next=remaining.merge(dependent,-1,Integer::sum);if(next==0)ready.add(dependent);});}
    if(order.size()!=graph.size())throw new IllegalArgumentException("workflow graph contains a cycle");return order;
  }
  private static void detectCycles(Map<String,Stage> graph){topologicalOrder(graph);}
}
