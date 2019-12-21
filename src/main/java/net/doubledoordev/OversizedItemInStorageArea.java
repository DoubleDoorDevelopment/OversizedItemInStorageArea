package net.doubledoordev;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.logging.log4j.Logger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import net.dries007.tfc.api.capability.heat.CapabilityItemHeat;
import net.dries007.tfc.api.capability.heat.IItemHeat;
import net.dries007.tfc.api.capability.size.CapabilityItemSize;
import net.dries007.tfc.api.capability.size.IItemSize;
import net.dries007.tfc.util.Helpers;

@Mod(
        modid = OversizedItemInStorageArea.MOD_ID,
        name = OversizedItemInStorageArea.MOD_NAME,
        version = OversizedItemInStorageArea.VERSION
)
public class OversizedItemInStorageArea
{

    public static final String MOD_ID = "oversizediteminstoragearea";
    public static final String MOD_NAME = "OversizedItemInStorageArea";
    public static final String VERSION = "1.1.0";
    /**
     * This is the instance of your mod as created by Forge. It will never be null.
     */
    @Mod.Instance(MOD_ID)
    public static OversizedItemInStorageArea INSTANCE;
    static Logger log;

    /**
     * This is the first initialization event. Register tile entities here.
     * The registry events below will have fired prior to entry to this method.
     */
    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event)
    {
        log = event.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MOD_ID))
        {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
        }
    }

    @SubscribeEvent
    public void onInventoryClose(PlayerContainerEvent.Close event)
    {
        // Our inventory we are working with.
        Container container = event.getContainer();
        // raytrace to get the block the user is looking at.
        RayTraceResult rayTrace = null;
        EntityPlayer player = event.getEntityPlayer();
        World world = player.getEntityWorld();
        ArrayList<String> betterSlotClassNameList = new ArrayList<>(Arrays.asList(ModConfig.slotClassNames));
        ArrayList<String> betterContainerClassNameList = new ArrayList<>(Arrays.asList(ModConfig.containerClassNames));
        ArrayList<Slot> slotsToEffect = new ArrayList<>();

        // simple debug to get container class names.
        if (ModConfig.debug)
        {
            log.info(container.getClass());
        }

        //check the container as lots of slots in mods are the same general slot.
        if (!betterContainerClassNameList.contains(container.getClass().getName()))
        {
            //Remove this entry if people put it in cause a shit ton of stuff uses this.
            betterSlotClassNameList.remove("net.minecraft.inventory.InventoryBasic");
            // Check over every slot inside the inventory to get the slots we want to mess with.
            for (Slot slot : container.inventorySlots)
            {
                // simple debug to get the slot class names.
                if (ModConfig.debug)
                {
                    log.info(slot.inventory.getClass());
                }

                //If our list doesn't contain this slots class, we want to add it to the list of things to be burned or yeeted.
                if (!betterSlotClassNameList.contains(slot.inventory.getClass().getName()))
                    slotsToEffect.add(slot);
            }

            //Loop over each slot that needs to be effected.
            for (Slot slot : slotsToEffect)
            {
                // make sure our slot has a stack.
                if (slot.getHasStack())
                {
                    // get the stack from the slot.
                    ItemStack stack = slot.getStack();
                    ItemStack stackHolder = stack.copy();
                    // get the ItemSize capability that holds the Size & Weight of the item.
                    IItemSize size = CapabilityItemSize.getIItemSize(stack);
                    // raytraced blockpos.
                    BlockPos tracedPos = null;
                    // hold our heat here, it's not guaranteed thus we need to be careful.
                    IItemHeat heatCapability = null;

                    // Get the heat of an item if it has it.
                    if (stack.hasCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null))
                    {
                        // store the heat cap because we have one
                        heatCapability = stack.getCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);
                    }

                    // see if we already have a raytrace result so we don't waist time on it or get another result because the player was pushed.
                    if (rayTrace == null)
                    {
                        // do the raytrace
                        rayTrace = Helpers.rayTrace(player, player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue(), 1);
                    }

                    // if the raytrace isn't null we have a block to "eject" from
                    if (rayTrace != null && rayTrace.typeOfHit != RayTraceResult.Type.MISS)
                    {
                        // block pos of our traced target.
                        tracedPos = rayTrace.getBlockPos();
                    }

                    // if we should do overheat stuff, Needs to be enabled, Traced pos needs to be valid, It needs to be a TE, The item needs a valid heatcap and the temp needs to be high enough.
                    if (ModConfig.overheatStartsFires && tracedPos != null && world.getTileEntity(tracedPos) != null && heatCapability != null && heatCapability.getTemperature() >= ModConfig.startFire)
                    {
                        // get the blockstate at the pos location
                        IBlockState target = world.getBlockState(tracedPos);
                        // make sure we have a heat cap to work with before getting the temperature and checking it against the cap.
                        if (target.getMaterial().getCanBurn())
                        {
                            // if we went over temp incinerate the block.
                            world.setBlockState(tracedPos, Blocks.FIRE.getDefaultState());
                            // notify the smart person that did this if needed.
                            if (ModConfig.shouldNotifyPlayer)
                                player.sendStatusMessage(new TextComponentString(ModConfig.overheatMessage), ModConfig.sendMessageToActionBar);
                        }
                    }
                    // Check the size listed on the item.
                    if (size.getSize(stack).ordinal() >= ModConfig.maxSize)
                    {
                        // Check to see if our pos is valid and then if our block has a TE otherwise the player isn't looking in a block.
                        if (tracedPos != null && world.getTileEntity(tracedPos) != null)
                        {
                            // check to see if everything should be dumped.
                            if (ModConfig.yeetItAll)
                            {
                                // if it all needs to be dumped, dump only the slots we should be dealing with.
                                for (Slot yeetableStack : slotsToEffect)
                                {
                                    // make sure we have a stack to work with first.
                                    if (yeetableStack.getHasStack())
                                    {
                                        yeetItem(world, tracedPos, yeetableStack.getStack());
                                        yeetableStack.getStack().setCount(0);
                                    }
                                }
                            }
                            //otherwise drop only one.
                            else
                            {
                                yeetItem(world, tracedPos, stackHolder);
                                stack.setCount(0);
                            }
                        }
                        // spawn the item on the player if it's not a TE
                        else
                        {
                            // check if we need to yeet it all.
                            if (ModConfig.yeetItAll && tracedPos != null)
                            {
                                // if it all needs to be dumped, dump only the slots we should be dealing with.
                                for (Slot yeetableStack : slotsToEffect)
                                {
                                    // make sure we have a stack to work with first.
                                    if (yeetableStack.getHasStack())
                                    {
                                        yeetItem(world, tracedPos, yeetableStack.getStack());
                                        yeetableStack.getStack().setCount(0);
                                    }
                                }
                            }
                            //otherwise just yet the problem item.
                            else
                            {
                                yeetItem(world, player.getPosition(), stackHolder);
                                stack.setCount(0);
                            }
                        }
                        // tell the player about what it if configured to.
                        if (ModConfig.shouldNotifyPlayer)
                            player.sendStatusMessage(new TextComponentString(ModConfig.ejectMessage), ModConfig.sendMessageToActionBar);
                    }
                }
            }
        }
    }

    private void yeetItem(World world, BlockPos pos, ItemStack item)
    {
        float f = world.rand.nextFloat() * 0.8F + 0.1F;
        float f1 = world.rand.nextFloat() * 0.8F + 0.1F;
        float f2 = world.rand.nextFloat() * 0.8F + 0.1F;
        // the item to be spawned and thrown
        EntityItem entityitem = new EntityItem(world, pos.getX() + (double) f, pos.getY() + (double) f1, pos.getZ() + (double) f2, item.splitStack(world.rand.nextInt(21) + 10));
        //set a delay so the player doesn't instantly collect it if they are in the way
        entityitem.setPickupDelay(30);
        //Set the motion on the item
        entityitem.motionX = world.rand.nextGaussian() * 0.07D;
        entityitem.motionY = world.rand.nextGaussian() * 0.07D + 0.20000000298023224D;
        entityitem.motionZ = world.rand.nextGaussian() * 0.07D;
        // Spawn the entity with the constructed Entityitem.
        world.spawnEntity(entityitem);
    }
}
