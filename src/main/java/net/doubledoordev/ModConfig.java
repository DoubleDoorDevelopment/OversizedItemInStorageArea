package net.doubledoordev;

import net.minecraftforge.common.config.Config;

@Config(modid = OversizedItemInStorageArea.MOD_ID)
@Config.LangKey("oiisa.config.title")
public class ModConfig
{
    @Config.LangKey("oiisa.config.message.eject")
    @Config.Comment("The message displayed in-game when items are ejected from the inventory.")
    public static String ejectMessage = "This trunk could not handle your junk!";

    @Config.LangKey("oiisa.config.message.overheat")
    @Config.Comment("The message displayed in-game when items are ejected from the inventory.")
    public static String overheatMessage = "This trunk could not handle your junk!";

    @Config.LangKey("oiisa.config.message.location")
    @Config.Comment("Where the eject message is sent. True = Action bar message. False = Chat Message.")
    public static Boolean sendMessageToActionBar = true;

    @Config.LangKey("oiisa.config.overheat.fire")
    @Config.Comment("Blocks that will burn up from a fire will be consumed by fire if an overheated item is placed inside. True = Fire. False = No Fire.")
    public static Boolean overheatStartsFires = true;

    @Config.LangKey("oiisa.config.message.send")
    @Config.Comment("If players should be notified about issues. True = Send players a message. False = no message.")
    public static Boolean shouldNotifyPlayer = true;

    @Config.LangKey("oiisa.config.yeetitall")
    @Config.Comment("Should everything be tossed everywhere if a single item is found to be oversized?")
    public static Boolean yeetItAll = true;

    @Config.LangKey("oiisa.config.debug")
    @Config.Comment("Use Debug to get the class names of the slots used inside the UI. Names are printed in the console.")
    public static Boolean debug = false;

    @Config.LangKey("oiisa.config.maxsize")
    @Config.Comment("The size to start kicking items out of an inventory at. 0 = Tiny, 1 = Very Small, 2 = Small, 3 = Normal, 4 = Large 5 = Very Large, 6 = Huge")
    @Config.RangeInt(min = 0, max = 6)
    public static int maxSize = 4;

    @Config.LangKey("oiisa.config.overheat.temp")
    @Config.Comment("Temperature that flammable inventories will combust.")
    @Config.RangeInt(min = 0, max = Integer.MAX_VALUE)
    public static int startFire = 600;

    @Config.LangKey("oiisa.config.allowed.slots")
    @Config.Comment("DO NOT ENTER InventoryBasic SLOTS HERE! Full Class path names of slots that are excluded from the checks. TURN ON DEBUG TO GET THESE NAMES")
    public static String[] slotClassNames = new String[3];
    @Config.LangKey("oiisa.config.allowed.inventories")
    @Config.Comment("Full Class path names of inventories that are excluded from the checks. TURN ON DEBUG TO GET THESE NAMES")
    public static String[] containerClassNames = new String[3];

    static
    {
        slotClassNames[0] = "net.minecraft.entity.player.InventoryPlayer";
        slotClassNames[1] = "net.minecraft.inventory.InventoryCrafting";
        slotClassNames[2] = "net.minecraft.inventory.InventoryCraftResult";
    }

    static
    {
        containerClassNames[0] = "net.dries007.tfc.objects.container.ContainerAnvilTFC";
        containerClassNames[1] = "net.dries007.tfc.objects.container.ContainerBarrel";
    }
}
