package de.dresdenairlines;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.UUID;

public final class PlayerWallet {
    private final DresdenAirlines plugin; private final File file; private final YamlConfiguration data;
    PlayerWallet(DresdenAirlines plugin){this.plugin=plugin; this.file=new File(plugin.getDataFolder(),"passenger-wallets.yml"); this.data=YamlConfiguration.loadConfiguration(file);}
    public double balance(UUID u){return data.getDouble(u+".money", plugin.getConfig().getDouble("passengers.starting-money",10000));}
    public boolean withdraw(UUID u,double amount){if(amount<0||balance(u)<amount)return false; data.set(u+".money",balance(u)-amount); save(); return true;}
    public void deposit(UUID u,double amount){if(amount<=0)return; data.set(u+".money",balance(u)+amount); save();}
    public void save(){try{data.save(file);}catch(Exception e){plugin.getLogger().warning("Could not save passenger wallets: "+e.getMessage());}}
}
