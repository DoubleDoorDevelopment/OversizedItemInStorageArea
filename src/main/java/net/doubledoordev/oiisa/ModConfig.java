package net.doubledoordev.oiisa;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static net.doubledoordev.oiisa.OversizedItemInStorageArea.INSTANCE;
import static net.doubledoordev.oiisa.OversizedItemInStorageArea.MOD_ID;

@Config(modid = MOD_ID, category = "All")
@Mod.EventBusSubscriber(modid = MOD_ID)
@Config.LangKey("oiisa.config.title")
public class ModConfig
{
    @Config.Name("Debug Options")
    @Config.Comment("Debug options.")
    public static final DebugOptions debugOptions = new DebugOptions();
    @Config.Name("Size Options")
    @Config.Comment("Size restrictions and settings for inventories/slots.")
    public static final SizeLimitOptions sizeLimitOptions = new SizeLimitOptions();
    @Config.Name("Weight Options")
    @Config.Comment("Weight restrictions and settings for inventories/slots.")
    public static final WeightLimitOptions weightLimitOptions = new WeightLimitOptions();
    @Config.Name("Overheat Options")
    @Config.Comment("Overheat settings for inventories")
    public static final OverheatOptions overheatOptions = new OverheatOptions();

    @Config.LangKey("oiisa.config.blocked.slots")
    @Config.Comment("DO NOT ENTER InventoryBasic SLOTS HERE! Full Class path names of slots that are excluded from ALL checks. TURN ON DEBUG TO GET THESE NAMES")
    public static String[] slotClassNames = new String[3];

    static
    {
        slotClassNames[0] = "net.minecraft.entity.player.InventoryPlayer";
        slotClassNames[1] = "net.minecraft.inventory.InventoryCrafting";
        slotClassNames[2] = "net.minecraft.inventory.InventoryCraftResult";
    }

    @Config.LangKey("oiisa.config.blocked.items")
    @Config.Comment("Items that are excluded from ALL checks. F3+H to get names.")
    public static String[] ignoredItems = new String[1];

    static
    {
        ignoredItems[0] = "minecraft:dirt";
    }

    public static class DebugOptions
    {
        @Config.LangKey("oiisa.config.debug")
        @Config.Comment("Turns on/off ALL debugging.")
        public Boolean debug = false;

        @Config.LangKey("oiisa.config.slot.debug")
        @Config.Comment("Enable/Disable the debug logging of slots. REQUIRES DEBUG TO BE ON!")
        public Boolean slotDebug = false;

        @Config.LangKey("oiisa.config.size.debug")
        @Config.Comment("Enable/Disable the debug logging of size related things. REQUIRES DEBUG TO BE ON!")
        public Boolean sizeDebug = false;

        @Config.LangKey("oiisa.config.weight.debug")
        @Config.Comment("Enable/Disable the debug logging of weight related things. REQUIRES DEBUG TO BE ON!")
        public Boolean weightDebug = false;

        @Config.LangKey("oiisa.config.heat.debug")
        @Config.Comment("Enable/Disable the debug logging of heat related things. REQUIRES DEBUG TO BE ON!")
        public Boolean heatDebug = false;

    }

    public static class SizeLimitOptions
    {
        @Config.LangKey("oiisa.config.size.message.eject")
        @Config.Comment("The message displayed in-game when items are ejected from the inventory.")
        public String sizeEjectMessage = "This trunk could not handle your junk!";

        @Config.LangKey("oiisa.config.size.message.location")
        @Config.Comment({"Where the eject message is sent.",
                "True = Action bar message. False = Chat Message."
        })
        public Boolean sizeActionBarMessage = true;

        @Config.LangKey("oiisa.config.size.message.send")
        @Config.Comment({"If players should be notified about issues.",
                "True = Send players a message. False = No message."
        })
        public Boolean sizeNotifyPlayer = true;

        @Config.LangKey("oiisa.config.size.yeetitall")
        @Config.Comment("Should everything be tossed everywhere if a single item is found to be over sized?")
        public Boolean sizeYeetItAll = true;

        @Config.LangKey("oiisa.config.size.whitelist")
        @Config.Comment({"Changes the default checks from whitelist to blacklist. ",
                "True = sizeContainers inventory are checked, False = sizeContainers inventory are NOT checked."
        })
        public Boolean sizeWhitelist = false;

        @Config.LangKey("oiisa.config.size.max")
        @Config.Comment({"This size and over are oversized and get ejected.",
                "Use sizeInvntoryMap for fine control per inventory.",
                "0 = Tiny, 1 = Very Small, 2 = Small, 3 = Normal, 4 = Large 5 = Very Large, 6 = Huge"
        })
        @Config.RangeInt(min = 0, max = 6)
        public int maxSize = 4;

        @Config.LangKey("oiisa.config.size.inventories")
        @Config.Comment("Full Class path names of inventories that are ignored/checked (depends on sizeBlacklist) for oversize items. TURN ON DEBUG TO GET THESE NAMES")
        public String[] sizeContainers = new String[8];

        {
            //TFC containers
            sizeContainers[0] = "net.dries007.tfc.objects.container.ContainerAnvilTFC";
            sizeContainers[1] = "net.dries007.tfc.objects.container.ContainerBarrel";
            sizeContainers[2] = "net.dries007.tfc.objects.container.ContainerLogPile";
            sizeContainers[3] = "net.dries007.tfc.objects.container.ContainerQuern";
            sizeContainers[4] = "net.dries007.tfc.objects.container.ContainerFirePit";
            sizeContainers[5] = "net.dries007.tfc.objects.container.ContainerCharcoalForge";
            sizeContainers[6] = "net.dries007.tfc.objects.container.ContainerCrucible";
            //FTB Utils containers
            sizeContainers[7] = "com.feed_the_beast.ftbutilities.command.InvSeeInventory";
        }

        @Config.LangKey("oiisa.config.size.inventoryarray")
        @Config.Comment({"Full Class path names of inventories that are checked for size limits. TURN ON DEBUG TO GET THE NAMES",
                "Size to start kicking items out of an inventory at. 0 = Tiny, 1 = Very Small, 2 = Small, 3 = Normal, 4 = Large 5 = Very Large, 6 = Huge",
                "Ignored inventories WILL NOT get checked! Adding them here DOES NOT MAKE THEM BE CHECKED!"
        })
        public String[] sizeInventoryArray = new String[1];

        {
            sizeInventoryArray[0] = "net.dries007.tfc.objects.container.ContainerChestTFC, 3";
        }
    }

    public static class WeightLimitOptions
    {
        @Config.LangKey("oiisa.config.weight.message.eject")
        @Config.Comment("The message displayed in-game when items are ejected from the inventory.")
        public String weightEjectMessage = "This trunk could not handle your junk!";

        @Config.LangKey("oiisa.config.weight.message.location")
        @Config.Comment({"Where the eject message is sent.",
                "True = Action bar message. False = Chat Message."
        })
        public Boolean weightActionBarMessage = true;

        @Config.LangKey("oiisa.config.weight.message.send")
        @Config.Comment({"If players should be notified about issues.",
                "True = Send players a message. False = no message."
        })
        public Boolean weightNotifyPlayer = true;

        @Config.LangKey("oiisa.config.weight.yeetitall")
        @Config.Comment("Should everything be tossed everywhere if a inventory is found to be over weight?")
        public Boolean weightYeetItAll = true;

        @Config.LangKey("oiisa.config.weight.very_light.value")
        @Config.Comment("How much a Very Light item weighs. THIS IS PER ITEM, NOT PER STACK!")
        public int veryLightItemWeight = 1;

        @Config.LangKey("oiisa.config.weight.light.value")
        @Config.Comment("How much a Light item weighs. THIS IS PER ITEM, NOT PER STACK!")
        public int lightItemWeight = 3;

        @Config.LangKey("oiisa.config.weight.medium.value")
        @Config.Comment("How much a Medium item weighs. THIS IS PER ITEM, NOT PER STACK!")
        public int mediumItemWeight = 5;

        @Config.LangKey("oiisa.config.weight.heavy.value")
        @Config.Comment("How much a Heavy item weighs. THIS IS PER ITEM, NOT PER STACK!")
        public int heavyItemWeight = 10;

        @Config.LangKey("oiisa.config.weight.very_heavy.value")
        @Config.Comment("How much a Very Heavy item weighs. THIS IS PER ITEM, NOT PER STACK!")
        public int veryHeavyItemWeight = 15;

        @Config.LangKey("oiisa.config.weight.inventoryarray")
        @Config.Comment({"Full Class path names of inventories that are checked for weight limits. TURN ON DEBUG TO GET THE NAMES",
                "Number = Max Weight for Inventory.",
                "Example: net.dries007.tfc.objects.container.ContainerLogPile, 20"
        })

        public String[] weightInventoryArray = new String[1];

        {
            weightInventoryArray[0] = "net.dries007.tfc.objects.container.ContainerChestTFC, 2000";
        }
    }

    public static class OverheatOptions
    {
        @Config.LangKey("oiisa.config.heat.makefire")
        @Config.Comment({"Blocks that will burn up from a fire will be consumed by fire if an overheated item is placed inside.",
                "True = Fire. False = No Fire."
        })
        public Boolean heatStartsFires = true;

        @Config.LangKey("oiisa.config.heat.message")
        @Config.Comment("The message displayed in-game when items are too hot for the inventory.")
        public String heatMessage = "That's Hot!";

        @Config.LangKey("oiisa.config.heat.message.location")
        @Config.Comment({"Where the eject message is sent.",
                "True = Action bar message. False = Chat Message."})
        public Boolean heatActionBarMessage = true;

        @Config.LangKey("oiisa.config.heat.message.send")
        @Config.Comment({"If players should be notified about issues.",
                "True = Send players a message. False = no message."
        })
        public Boolean heatNotifyPlayer = true;

        @Config.LangKey("oiisa.config.heat.temp")
        @Config.Comment("Temperature that flammable inventories will combust.")
        @Config.RangeInt(min = 0, max = Integer.MAX_VALUE)
        public int heatToStartFire = 600;

        @Config.LangKey("oiisa.config.heat.disabledinventory")
        @Config.Comment({"Full Class path names of inventories that are immune to burning. TURN ON DEBUG TO GET THE NAMES"
        })
        public String[] disabledInventories = new String[2];
        @Config.LangKey("oiisa.config.heat.incinerateplayer")
        @Config.Comment("Temperature that players will instantly die with equipped hot armor equipped.")
        @Config.RangeInt(min = 0, max = Integer.MAX_VALUE)
        public int heatToIncineratePlayer = 150;
        @Config.LangKey("oiisa.config.heat.burnplayer")
        @Config.Comment("Temperature that players will instantly combust with hot armor equipped.")
        @Config.RangeInt(min = 0, max = Integer.MAX_VALUE)
        public int heatToCombustPlayer = 70;
        @Config.LangKey("oiisa.config.heat.incineratedestroysitems")
        @Config.Comment({"Does a player incinerating there self destroy burnable items they have in there inventory?",
                "You can add custom options by giving items fuel time via other mods. Anything greater than 0 will combust."})
        public boolean incinerateHeldBurnableItems = true;
        @Config.LangKey("oiisa.config.heat.incineratedplayerstartsfire")
        @Config.Comment({"Does a player dying from incinerating there self start a fire where they stand?"})
        public boolean incineratedPlayersStartFires = true;
        @Config.LangKey("oiisa.config.heat.incinerateitemstoash")
        @Config.Comment({"Should burnt items be converted to ash at a 1 to 1 ratio?"})
        public boolean incinerateitemstoash = true;

        {
            disabledInventories[0] = "net.dries007.tfc.objects.container.ContainerBarrel";
            disabledInventories[1] = "net.dries007.tfc.objects.container.ContainerFirePit";
        }
    }

    @Mod.EventBusSubscriber
    public static class SyncConfig
    {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
        {
            if (event.getModID().equals(MOD_ID))
            {
                ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
                INSTANCE.splitConfig();
            }
        }
    }
}
