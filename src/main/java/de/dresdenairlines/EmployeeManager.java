package de.dresdenairlines;
import java.util.*;
public final class EmployeeManager {
 private final DresdenAirlines p;
 EmployeeManager(DresdenAirlines p){this.p=p;}
 public Employee hire(Airline a,String role){if(a==null)return null; Role r=Role.from(role); if(r==null||a.money<r.cost)return null; Employee e=new Employee(UUID.randomUUID(),r.display, r.display,60,r.salary,1,a.homeAirport,System.currentTimeMillis()); a.money-=r.cost; a.employees.add(e); p.storage.save(); return e;}
 public boolean fire(Airline a,UUID id){if(a==null||id==null)return false; boolean ok=a.employees.removeIf(e->e.id.equals(id)); if(ok)p.storage.save(); return ok;}
 public void salaryTick(){long now=System.currentTimeMillis(); long interval=p.getConfig().getLong("employees.salary-interval-seconds",3600)*1000L; for(Airline a:p.storage.airlines.values()) for(Employee e:a.employees) if(now-e.lastPaidAt>=interval){double due=e.salary*e.level; if(a.money>=due)a.money-=due; else a.reputation=Math.max(0,a.reputation-.5); e.lastPaidAt=now;}}
 public enum Role {PILOT("Pilot",3500,1800),GROUND("Bodenpersonal",1800,900),MECHANIC("Mechaniker",2600,1300),CABIN("Flugbegleiter",2200,1100),ATC("Fluglotse",3200,1600); public final String display; public final double cost,salary; Role(String d,double c,double s){display=d;cost=c;salary=s;} static Role from(String s){for(Role r:values())if(r.display.equalsIgnoreCase(s))return r;return null;}}
}
