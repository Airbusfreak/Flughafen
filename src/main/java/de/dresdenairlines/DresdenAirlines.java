package de.dresdenairlines;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

public final class DresdenAirlines extends JavaPlugin {
 public NamespacedKey npcKey;
 public Storage storage;
 public RoutePreGenerator preGenerator;
 public AirportManager airports;
 public StationManager stations;
 public GateManager gates;
 public AirportLevelManager airportLevels;
 public EmployeeManager employees;
 public SimulationManager simulation;
 public AirlineManager airlines;
 public AirlineGUI airlineGUI;
 public LiveryGUI liveryGUI;
 public EmployeeGUI employeeGUI;
 public AirportGUI airportGUI;
 public TransitGUI transitGUI;
 public FlightGUI flightGUI;
 public FlightDetailsGUI flightDetailsGUI;
 public FlightManager flights;
 public FlightRenderer renderer;
 public PassengerManager passengers;

 public void onEnable(){
  saveDefaultConfig();
  npcKey=new NamespacedKey(this,"simulated_passenger_flight");
  storage=new Storage(this);
  storage.load();
  airports=new AirportManager(this);
  stations=new StationManager(this);
  gates=new GateManager();
  airportLevels=new AirportLevelManager(this);
  employees=new EmployeeManager(this);
  simulation=new SimulationManager(this);
  preGenerator=new RoutePreGenerator(this);
  airlines=new AirlineManager(this);
  airlineGUI=new AirlineGUI(this);
  liveryGUI=new LiveryGUI(this);
  employeeGUI=new EmployeeGUI(this);
  airportGUI=new AirportGUI(this);
  transitGUI=new TransitGUI(this);
  passengers=new PassengerManager(this);
  flightGUI=new FlightGUI(this);
  flightDetailsGUI=new FlightDetailsGUI(this);
  flights=new FlightManager(this,airports);
  renderer=new FlightRenderer(this);

  getServer().getPluginManager().registerEvents(airports,this);
  getServer().getPluginManager().registerEvents(stations,this);
  getServer().getPluginManager().registerEvents(airlineGUI,this);
  getServer().getPluginManager().registerEvents(liveryGUI,this);
  getServer().getPluginManager().registerEvents(employeeGUI,this);
  getServer().getPluginManager().registerEvents(airportGUI,this);
  getServer().getPluginManager().registerEvents(transitGUI,this);
  getServer().getPluginManager().registerEvents(flightGUI,this);
  getServer().getPluginManager().registerEvents(flightDetailsGUI,this);
  getServer().getPluginManager().registerEvents(passengers,this);

  getCommand("airline").setExecutor(new Commands(this));
  getCommand("airport").setExecutor(new Commands(this));
  getCommand("flight").setExecutor(new Commands(this));

  getServer().getScheduler().runTaskLater(this,()->{
   airports.load();
   stations.load();
   airports.createInitial();
   airportLevels.load();
   gates.rebuild(storage.flights.values());
  },40);

  getServer().getScheduler().runTaskTimer(this,()->{
   airlines.economyTick();
   employees.salaryTick();
   passengers.tick();
   passengers.animateNpcs();
   renderer.tick();
   stations.tick();
   simulation.tick();
   airports.automaticGenerationTick();
  },20,20);

  getServer().getScheduler().runTaskTimer(this,storage::save,20*300,20*300);
 }

 public void onDisable(){
  if(airports!=null)airports.save();
  if(stations!=null)stations.save();
  if(airportLevels!=null)airportLevels.save();
  storage.save();
  if(passengers!=null)passengers.wallet.save();
 }
}
