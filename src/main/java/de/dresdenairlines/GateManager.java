package de.dresdenairlines;
import java.util.*;
/** Dynamic airport stand allocation. Gates are resources, not permanently owned by flights or airlines. */
public final class GateManager {
 private final Map<String,String> flightDeparture=new HashMap<>(), flightArrival=new HashMap<>(), occupied=new HashMap<>();
 public String assignDeparture(Flight f){String g=find(f.from,f.aircraft.type);if(g!=null){flightDeparture.put(f.id,g);occupied.put(key(f.from,g),f.id);f.departureGateId=g;}return g;}
 public String assignArrival(Flight f){String g=find(f.to,f.aircraft.type);if(g!=null){flightArrival.put(f.id,g);occupied.put(key(f.to,g),f.id);f.arrivalGateId=g;}return g;}
 private String find(Airport a,String type){for(String g:a.gates())if(!occupied.containsKey(key(a,g))&&compatible(g,type))return g;return null;}
 private boolean compatible(String gate,String type){try{int n=Integer.parseInt(gate.replaceAll("\\D",""));return n>=4||(!type.startsWith("A330")&&!type.startsWith("A350"));}catch(Exception e){return true;}}
 private String key(Airport a,String g){return a.id()+":"+g;}
 public void releaseDeparture(Flight f){if(f.departureGateId!=null){occupied.remove(key(f.from,f.departureGateId));flightDeparture.remove(f.id);f.departureGateId=null;}}
 public void releaseArrival(Flight f){if(f.arrivalGateId!=null){occupied.remove(key(f.to,f.arrivalGateId));flightArrival.remove(f.id);f.arrivalGateId=null;}}
 public void releaseAll(Flight f){releaseDeparture(f);releaseArrival(f);}
 public boolean hasFreeGate(Airport a){return a.gates().stream().anyMatch(g->!occupied.containsKey(key(a,g)));}
 public void rebuild(Collection<Flight> fs){occupied.clear();flightDeparture.clear();flightArrival.clear();for(Flight f:fs){if(f.status==FlightStatus.SCHEDULED||f.status==FlightStatus.BOARDING)assignDeparture(f);else if(f.status==FlightStatus.APPROACH)assignArrival(f);}}
}
