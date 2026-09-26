package de.dresdenairlines;

import org.bukkit.command.*; import org.bukkit.entity.Player; import org.bukkit.Location; import java.util.*;

public final class Commands implements CommandExecutor {
 final DresdenAirlines p; Commands(DresdenAirlines p){this.p=p;}
 public boolean onCommand(CommandSender s,Command c,String l,String[] a){
  if(!(s instanceof Player pl))return true;
  if(c.getName().equalsIgnoreCase("flight")){
   if(a.length==0){p.flightGUI.open(pl);return true;}
   if(a[0].equalsIgnoreCase("money")){pl.sendMessage("§6Kontostand: §f"+p.passengers.money(pl.getUniqueId()));return true;}
   if(a[0].equalsIgnoreCase("book")&&a.length>=2){String err=p.passengers.bookNext(pl,a[1].toUpperCase());if(err!=null)pl.sendMessage(err);return true;}
   if(a[0].equalsIgnoreCase("give")&&a.length>=3&&pl.hasPermission("dresdenairlines.admin")){try{Player t=org.bukkit.Bukkit.getPlayerExact(a[1]);double amount=Double.parseDouble(a[2]);if(t==null||amount<=0){pl.sendMessage("§cSpieler oder Betrag ungültig.");return true;}p.passengers.wallet.deposit(t.getUniqueId(),amount);pl.sendMessage("§a"+amount+" $ an "+t.getName()+" gegeben.");t.sendMessage("§aDu hast "+amount+" $ erhalten.");}catch(Exception ex){pl.sendMessage("§cBetrag ungültig.");}return true;}
   if(a[0].equalsIgnoreCase("take")&&a.length>=3&&pl.hasPermission("dresdenairlines.admin")){try{Player t=org.bukkit.Bukkit.getPlayerExact(a[1]);double amount=Double.parseDouble(a[2]);if(t==null||amount<=0){pl.sendMessage("§cSpieler oder Betrag ungültig.");return true;}boolean ok=p.passengers.wallet.withdraw(t.getUniqueId(),amount);pl.sendMessage(ok?"§c"+amount+" $ von "+t.getName()+" genommen.":"§cSpieler hat nicht genug Geld.");if(ok)t.sendMessage("§cDir wurden "+amount+" $ abgezogen.");}catch(Exception ex){pl.sendMessage("§cBetrag ungültig.");}return true;}
   pl.sendMessage("§b/flight §7– Flüge auswählen | §b/flight book <Ziel> §7– direkt buchen | §b/flight money");return true;
  }
  if(c.getName().equalsIgnoreCase("airline")){
   if(a.length==0){p.airlineGUI.open(pl);return true;}
   Airline al=p.storage.airlines.get(pl.getUniqueId());
   switch(a[0].toLowerCase()){
    case "create"->{if(a.length<2){pl.sendMessage("§c/airline create <Name> [Code]");return true;}if(al!=null){pl.sendMessage("§cDu hast bereits eine Airline.");return true;}String code=a.length>2?a[2].toUpperCase():"AIR";if(code.length()>4){pl.sendMessage("§cCode max. 4 Zeichen.");return true;}al=new Airline(pl.getUniqueId(),a[1],code,"DRE",p.getConfig().getDouble("economy.starting-money"));p.storage.airlines.put(pl.getUniqueId(),al);p.storage.save();pl.sendMessage("§aAirline gegründet.");p.airlineGUI.open(pl);}
    case "buy"->{if(al==null){pl.sendMessage("§cErstelle zuerst eine Airline.");return true;}if(a.length<2){pl.sendMessage("§cTypen: A220-300, A320, A330-300, A350-900, ERJ195");return true;}Aircraft ac=p.airlines.buy(al,a[1]);pl.sendMessage(ac==null?"§cKauf nicht möglich (Geld/Typ).":"§aGekauft: "+ac.type+" • "+ac.registration+" • "+ac.id);}
    case "route"->{if(al==null||a.length<5){pl.sendMessage("§c/airline route <von> <nach> <Flugzeug-ID> <Preis>");return true;}try{boolean ok=p.airlines.addRoute(al,a[1].toUpperCase(),a[2].toUpperCase(),a[3],Integer.parseInt(a[4]),300);pl.sendMessage(ok?"§aRoute eingerichtet.":"§cRoute konnte nicht eingerichtet werden.");}catch(Exception ex){pl.sendMessage("§cUngültige Eingabe.");}}
    case "fleet"->{if(al==null){pl.sendMessage("§cKeine Airline.");return true;}for(Aircraft x:al.fleet)pl.sendMessage("§b"+x.id+" §7"+x.type+" §8"+x.registration+" §7"+x.seats+" Sitze");}
    case "routes"->{if(al==null){pl.sendMessage("§cKeine Airline.");return true;}for(Route r:al.routes)pl.sendMessage("§e"+r.from+" → "+r.to+" §7Preis "+r.ticketPrice+"$ §8"+r.aircraftId);}
    case "info"->{if(al==null){pl.sendMessage("§cKeine Airline.");return true;}long active=p.storage.flights.values().stream().filter(f->f.airline==al&&f.status!=FlightStatus.LANDED).count();long pax=p.storage.flights.values().stream().filter(f->f.airline==al).mapToLong(f->f.booked+f.npcBooked).sum();pl.sendMessage("§b"+al.name+" §7("+al.code+") §f"+String.format("%.0f",al.money)+"$ §7Ruf "+String.format("%.1f",al.reputation)+" §7| Flotte "+al.fleet.size()+" | aktive Flüge "+active+" | Passagiere "+pax);}
    case "ranking"->{List<Airline> list=new ArrayList<>(p.storage.airlines.values());list.sort(Comparator.comparingDouble((Airline x)->x.money+x.reputation*10000+x.fleet.size()*1000).reversed());pl.sendMessage("§6═══ Airline-Rangliste ═══");int rank=1;for(Airline x:list.stream().limit(10).toList())pl.sendMessage("§e"+rank+++". §f"+x.name+" §7| §a"+String.format("%.0f",x.money)+"$ §7| Ruf §e"+String.format("%.1f",x.reputation)+" §7| Flotte §f"+x.fleet.size());}
   }return true;
  }
  if(a.length==0){pl.sendMessage("§b/airport list | locate [ID/Name] | info | generate | stations");return true;}
  if(a[0].equalsIgnoreCase("stations")){for(StationManager.Station st:p.stations.stations.values())pl.sendMessage("§b"+st.id()+" §7"+st.name()+" §8["+st.station().getBlockX()+", "+st.station().getBlockY()+", "+st.station().getBlockZ()+"]");}
  else if(a[0].equalsIgnoreCase("list"))for(Airport x:p.airports.airports.values())pl.sendMessage("§e"+x.id()+" §7- "+x.name()+" §8["+x.center().getBlockX()+", "+x.center().getBlockY()+", "+x.center().getBlockZ()+"]");
  else if(a[0].equalsIgnoreCase("locate")){
   if(a.length==1){Airport x=p.airports.nearest(pl.getLocation()); if(x!=null) sendLocate(pl,x); else pl.sendMessage("§cKein Flughafen gefunden.");}
   else {List<Airport> found=p.airports.find(String.join(" ",Arrays.copyOfRange(a,1,a.length))); if(found.isEmpty()) pl.sendMessage("§cKein Flughafen gefunden."); else {for(Airport x:found.stream().limit(5).toList()) sendLocate(pl,x);}}
  }
  else if(a[0].equalsIgnoreCase("info")){Airport x=p.airports.nearest(pl.getLocation());if(x!=null){pl.sendMessage(p.airportLevels.info(x));pl.sendMessage("§7Gates: §f"+x.gates().size());}}
  else if(a[0].equalsIgnoreCase("generate")&&pl.hasPermission("dresdenairlines.admin")){p.airports.randomGenerateAround(pl.getLocation());pl.sendMessage("§aRegion geprüft.");}
  return true;
 }
 private void sendLocate(Player pl,Airport x){
  Location c=x.center(); double d=pl.getWorld()==c.getWorld()?Math.sqrt(pl.getLocation().distanceSquared(c)):Double.NaN;
  pl.sendMessage("§aFlughafen gefunden: §f"+x.name()+" §7("+x.id()+") §8X="+c.getBlockX()+" Y="+c.getBlockY()+" Z="+c.getBlockZ()+(Double.isNaN(d)?"":" §7Entfernung: §e"+Math.round(d)+" Blöcke"));
 }
}
