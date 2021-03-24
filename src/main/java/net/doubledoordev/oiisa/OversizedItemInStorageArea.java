package net.doubledoordev.oiisa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.DamageSource;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import net.dries007.tfc.api.capability.heat.CapabilityItemHeat;
import net.dries007.tfc.api.capability.heat.IItemHeat;
import net.dries007.tfc.api.capability.size.CapabilityItemSize;
import net.dries007.tfc.util.Helpers;

import static net.dries007.tfc.objects.items.ItemsTFC.WOOD_ASH;

@Mod(
        modid = OversizedItemInStorageArea.MOD_ID,
        name = OversizedItemInStorageArea.MOD_NAME,
        version = OversizedItemInStorageArea.VERSION
)
public class OversizedItemInStorageArea
{

    public static final String MOD_ID = "oversizediteminstoragearea";
    public static final String MOD_NAME = "OversizedItemInStorageArea";
    public static final String VERSION = "2.2.2";

    private static final Pattern splitter = Pattern.compile("\\b([A-Za-z0-9:._\\s]+)");

    DamageSource playerIncinerator = new DamageSource("oiisaincinerator").setDamageBypassesArmor().setDamageIsAbsolute();

    Map<String, Integer> weightMap = new HashMap<>();
    Map<String, Integer> containerSizeOverideMap = new HashMap<>();

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
        splitConfig();
    }

    //Why the config thing didn't support maps beats me... yay bullshit!
    public void splitShit(String[] configInput, Map<String, Integer> outputSave)
    {
        ArrayList<String> array = new ArrayList<>();
        String key;
        if (configInput.length > 0)
        {
            for (String configEntry : configInput)
            {
                Matcher matcher = splitter.matcher(configEntry);
                while (matcher.find())
                {
                    array.add(matcher.group().trim());
                }
                if (!array.isEmpty())
                {
                    key = array.get(0);
                    array.remove(0);
                    outputSave.put(key, Integer.valueOf(array.get(0)));
                    array.clear();
                }
            }
        }
    }

    @SubscribeEvent
    public void playerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            incineratePlayer(event.player);
    }

    @SubscribeEvent
    public void onInventoryClose(PlayerContainerEvent.Close event)
    {
        // Our inventory we are working with.
        Container container = event.getContainer();
        EntityPlayer player = event.getEntityPlayer();
        World world = player.getEntityWorld();
        String containerName = container.getClass().getName();

        // raytrace to get the block the user is looking at.
        BlockPos tracedPos = getTracedPos(Helpers.rayTrace(player, player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue(), 1));
        ArrayList<String> slotClassNameList = new ArrayList<>(Arrays.asList(ModConfig.slotClassNames));
        ArrayList<String> containerClassNameList = new ArrayList<>(Arrays.asList(ModConfig.sizeLimitOptions.sizeContainers));
        ArrayList<String> blockedItemNameList = new ArrayList<>(Arrays.asList(ModConfig.ignoredItems));
        ArrayList<Slot> slotsToEffect = new ArrayList<>();
        int maxSize = ModConfig.sizeLimitOptions.maxSize;

        //Specialty handling of the smol vessel due to something eating preservation statuses on food items.
        if (event.getContainer().getClass().getName().equals("net.dries007.tfc.objects.container.ContainerSmallVessel"))
            return;

        // Removes unwanted entries in the slot list.
        cleanSlotList(container, slotClassNameList, slotsToEffect, containerName, player);

        //Look for items that should start fires.
        checkItemHeat(tracedPos, containerName, world, slotsToEffect, player, blockedItemNameList);

        // Checks what mode the size list is in.
        if (ModConfig.sizeLimitOptions.sizeWhitelist)
        {
            // If the list contains the name enforce the size restriction.
            if (containerClassNameList.contains(containerName))
            {
                // Check the size.
                doSizeCheck(player, world, containerName, tracedPos, blockedItemNameList, slotsToEffect, maxSize);
            }
        }
        // If the list doesn't contain the name as we are in blacklist mode.
        else if (!containerClassNameList.contains(containerName))
        {
            // Check the size.
            doSizeCheck(player, world, containerName, tracedPos, blockedItemNameList, slotsToEffect, maxSize);
        }

        // See if the inventory needs a size applied to it.
        if (INSTANCE.weightMap.containsKey(containerName))
        {
            // The items that exceed the weight limit.
            ArrayList<Slot> overWeightItems = checkWeight(slotsToEffect, containerName, blockedItemNameList);

            // Config check
            if (ModConfig.weightLimitOptions.weightYeetItAll)
            {
                // make sure the array isn't empty
                if (!overWeightItems.isEmpty())
                {
                    // Drop it all
                    yeetAll(slotsToEffect, world, tracedPos);

                    // tell the player about what it if configured to.
                    if (ModConfig.weightLimitOptions.weightNotifyPlayer)
                        player.sendStatusMessage(new TextComponentString(ModConfig.weightLimitOptions.weightEjectMessage), ModConfig.weightLimitOptions.weightActionBarMessage);
                }
            }
            else
            {
                // make sure the array isn't empty
                if (!overWeightItems.isEmpty())
                {
                    // drop only the items that are over the weight limit.
                    for (Slot yeetThisSlot : overWeightItems)
                    {
                        yeetItem(world, tracedPos, yeetThisSlot);

                        // tell the player about what it if configured to.
                        if (ModConfig.weightLimitOptions.weightNotifyPlayer)
                            player.sendStatusMessage(new TextComponentString(ModConfig.weightLimitOptions.weightEjectMessage), ModConfig.weightLimitOptions.weightActionBarMessage);
                    }
                }
            }
        }
    }

    void splitConfig()
    {
        splitShit(ModConfig.weightLimitOptions.weightInventoryArray, weightMap);
        splitShit(ModConfig.sizeLimitOptions.sizeInventoryArray, containerSizeOverideMap);
    }

    private void doSizeCheck(EntityPlayer player, World world, String containerName, BlockPos tracedPos, ArrayList<String> blockedItemNameList, ArrayList<Slot> slotsToEffect, int maxSize)
    {
        // If the config overrides the default with a special level...
        if (INSTANCE.containerSizeOverideMap.containsKey(containerName))
        {
            // change it to match that override.
            maxSize = INSTANCE.containerSizeOverideMap.get(containerName);
        }
        // Do the check of size and heat.
        checkSize(slotsToEffect, tracedPos, world, player, maxSize, blockedItemNameList);
    }

    private void checkSize(ArrayList<Slot> slotsToEffect, BlockPos tracedPos, World world, EntityPlayer player, int maxSize, ArrayList<String> blockedItemNameList)
    {
        //Loop over each slot that needs to be acted on.
        for (Slot slot : slotsToEffect)
        {
            // make sure our slot has a stack.
            if (slot.getHasStack() && !blockedItemNameList.contains(slot.getStack().getItem().getRegistryName().toString()))
            {
                // get the stack from the slot.
                ItemStack stackToActOn = slot.getStack();
                // get the ItemSize capability that holds the Size & Weight of the item.
                int size = CapabilityItemSize.getIItemSize(stackToActOn).getSize(stackToActOn).ordinal();

                // Check the size listed on the item.
                if (size >= maxSize)
                {
                    doYeet(tracedPos, world, slotsToEffect, slot, ModConfig.sizeLimitOptions.sizeYeetItAll, player);

                    // tell the player about what it if configured to.
                    if (ModConfig.sizeLimitOptions.sizeNotifyPlayer)
                        player.sendStatusMessage(new TextComponentString(ModConfig.sizeLimitOptions.sizeEjectMessage), ModConfig.sizeLimitOptions.sizeActionBarMessage);
                }
            }
        }
    }

    private ArrayList<Slot> checkWeight(ArrayList<Slot> slotsToEffect, String containerName, ArrayList<String> blockedItemNameList)
    {
        int currentWeight = 0;
        int maxWeight = INSTANCE.weightMap.get(containerName);
        ArrayList<Slot> toYeet = new ArrayList<>();

        //Loop over the slots to effect.
        for (Slot slot : slotsToEffect)
        {
            // Make sure each slot has an item and isn't a blocked one.
            if (slot.getHasStack() && !blockedItemNameList.contains(slot.getStack().getItem().getRegistryName().toString()))
            {
                ItemStack itemStack = slot.getStack();
                //Get the weight based off the config and add it to the current weight.
                if (currentWeight < maxWeight)
                {
                    switch (CapabilityItemSize.getIItemSize(slot.getStack()).getWeight(itemStack).ordinal())
                    {
                        case 0:
                            currentWeight = ModConfig.weightLimitOptions.veryLightItemWeight * itemStack.getCount() + currentWeight;
                            break;
                        case 1:
                            currentWeight = ModConfig.weightLimitOptions.lightItemWeight * itemStack.getCount() + currentWeight;
                            break;
                        case 2:
                            currentWeight = ModConfig.weightLimitOptions.mediumItemWeight * itemStack.getCount() + currentWeight;
                            break;
                        case 3:
                            currentWeight = ModConfig.weightLimitOptions.heavyItemWeight * itemStack.getCount() + currentWeight;
                            break;
                        case 4:
                            currentWeight = ModConfig.weightLimitOptions.veryHeavyItemWeight * itemStack.getCount() + currentWeight;
                    }
                }
                else
                    toYeet.add(slot);
            }
        }
        if (ModConfig.debugOptions.debug && ModConfig.debugOptions.weightDebug)
        {
            log.info("Total Weight of Container: " + currentWeight + " Maximum weight allowed: " + maxWeight);
        }
        return toYeet;
    }

    private void cleanSlotList(Container container, ArrayList<String> slotClassNameList, ArrayList<Slot> slotsToEffect, String containerName, EntityPlayer player)
    {
        //Remove this entry if people put it in cause a shit ton of stuff uses this and warn them.
        if (slotClassNameList.remove("net.minecraft.inventory.InventoryBasic"))
        {
            log.warn("Ignoring basic slot! DON'T PUT \"net.minecraft.inventory.InventoryBasic\" IN YOUR CONFIG, LIKE THE CONFIG SAYS! THIS IS NOT A BUG!");
        }

        // simple debug to get the container class name.
        if (ModConfig.debugOptions.debug)
        {
            log.info(containerName);
        }

        // Check over every slot inside the inventory to get the slots we want to mess with.
        for (Slot slot : container.inventorySlots)
        {

            // simple debug to get the slot class names.
            if (ModConfig.debugOptions.debug && ModConfig.debugOptions.slotDebug)
            {
                log.info(slot.inventory.getClass());
            }

            //If the blacklist list doesn't contain this slot class and the slot can have items taken
            // we want to add it to the list of things to be burned or yeeted.
            if (!slotClassNameList.contains(slot.inventory.getClass().getName()) && slot.canTakeStack(player))
                slotsToEffect.add(slot);
        }
    }

    private void yeetItem(World world, BlockPos tracedPos, Slot yeetslot)
    {
        spawnYeetItem(world, tracedPos, yeetslot.getStack());
        yeetslot.getStack().setCount(0);
        yeetslot.inventory.markDirty();
    }

    private void yeetAll(ArrayList<Slot> slotsToEffect, World world, BlockPos tracedPos)
    {
        // if it all needs to be dumped, dump only the slots we should be dealing with.
        for (Slot slotToYeet : slotsToEffect)
        {
            // make sure we have a stack to work with first.
            if (slotToYeet.getHasStack())
            {
                yeetItem(world, tracedPos, slotToYeet);
            }
        }
    }

    private void doYeet(BlockPos tracedPos, World world, ArrayList<Slot> slotsToEffect, Slot yeetslot, Boolean selectiveYeet, EntityPlayer player)
    {
        // Check to see if our pos is valid and then if our block has a TE otherwise the player isn't looking in a block.
        if (tracedPos != null && world.getTileEntity(tracedPos) != null)
        {
            if (selectiveYeet)
                yeetItem(world, tracedPos, yeetslot);
            else
                yeetAll(slotsToEffect, world, tracedPos);
        }
        // spawn the item on the player if it's not a TE
        else
        {
            if (selectiveYeet)
                yeetItem(world, player.getPosition(), yeetslot);
            else
                yeetAll(slotsToEffect, world, player.getPosition());
        }
    }

    private BlockPos getTracedPos(RayTraceResult rayTrace)
    {
        // if the raytrace isn't null & we have a block to "eject" from
        if (rayTrace != null && rayTrace.typeOfHit != RayTraceResult.Type.MISS)
        {
            // block pos of our traced target.
            return rayTrace.getBlockPos();
        }
        return null;
    }

    private void checkItemHeat(BlockPos tracedPos, String containerName, World world, ArrayList<Slot> slotsToEffect, EntityPlayer player, ArrayList<String> blockedItemNameList)
    {
        ArrayList<String> disabledInvs = new ArrayList<>(Arrays.asList(ModConfig.overheatOptions.disabledInventories));
        if (ModConfig.overheatOptions.heatStartsFires && !disabledInvs.contains(containerName))
        {
            //Loop over the slots.
            for (Slot slot : slotsToEffect)
            {
                //If the stack has a heat capability and isn't on the ignore list we can get the cap off the item and check the heat.
                if (slot.getStack().hasCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null) && !blockedItemNameList.contains(slot.getStack().getItem().getRegistryName().toString()))
                {
                    IItemHeat heatCapability = slot.getStack().getCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);

                    // if we should do overheat stuff, Needs to be enabled, Traced pos needs to be valid, It needs to be a TE, The item needs a valid heatcap and the temp needs to be high enough.
                    if (tracedPos != null && world.getTileEntity(tracedPos) != null && heatCapability != null && heatCapability.getTemperature() >= ModConfig.overheatOptions.heatToStartFire)
                    {
                        if (ModConfig.debugOptions.debug && ModConfig.debugOptions.heatDebug)
                        {
                            log.info("Current Item Temperature: " + heatCapability.getTemperature() + " Maximum Temperature allowed: " + ModConfig.overheatOptions.heatToStartFire);
                        }
                        // get the blockstate at the pos location
                        IBlockState target = world.getBlockState(tracedPos);
                        // Does the material burn at this location?
                        if (target.getMaterial().getCanBurn())
                        {
                            // if we went over temp incinerate the block.
                            world.setBlockState(tracedPos, Blocks.FIRE.getDefaultState());
                            // notify the smart person that did this if needed.
                            if (ModConfig.overheatOptions.heatNotifyPlayer)
                                player.sendStatusMessage(new TextComponentString(ModConfig.overheatOptions.heatMessage), ModConfig.overheatOptions.heatActionBarMessage);
                        }
                        else
                        {
                            if (ModConfig.debugOptions.debug && ModConfig.debugOptions.heatDebug)
                            {
                                log.info("Fire was aborted as this block can't burn.");
                            }
                        }
                    }
                }
            }
        }
    }

    private void incineratePlayer(EntityPlayer player)
    {
        if (!player.isCreative() && !player.isSpectator())
        {
            NonNullList<ItemStack> playerMainInv = player.inventory.mainInventory;
            NonNullList<ItemStack> playerArmorInv = player.inventory.armorInventory;
            NonNullList<ItemStack> playerOffHandInv = player.inventory.offHandInventory;
            World world = player.getEntityWorld();
            BlockPos playerPos = player.getPosition();
            String playerDisplayName = player.getDisplayName().getFormattedText();

            ArrayList<ItemStack> ashToSpawn = new ArrayList<>();

            for (ItemStack item : playerArmorInv)
            {
                //If the stack has a heat capability and isn't on the ignore list we can get the cap off the item and check the heat.
                if (item.hasCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null))
                {
                    IItemHeat heatCapability = item.getCapability(CapabilityItemHeat.ITEM_HEAT_CAPABILITY, null);

                    //Make sure it's not null.
                    if (heatCapability != null)
                    {
                        float itemTemp = heatCapability.getTemperature();
                        //First check for incineration as it's the higher value (or should be)...
                        if (itemTemp >= ModConfig.overheatOptions.heatToIncineratePlayer)
                        {
                            //Check each inventory to incinerate items based off if they have a furnace fuel value.
                            if (ModConfig.overheatOptions.incinerateHeldBurnableItems)
                            {
                                for (ItemStack stack : playerMainInv)
                                {
                                    if (TileEntityFurnace.getItemBurnTime(stack) > 0)
                                    {
                                        if (ModConfig.overheatOptions.incinerateitemstoash)
                                        {
                                            ItemStack insultingAsh = new ItemStack(WOOD_ASH, stack.getCount());
                                            insultingAsh.setStackDisplayName("\u00a7r\u00a78" + stack.getDisplayName() + " Ash");
                                            ashToSpawn.add(insultingAsh);
                                        }
                                        stack.setCount(0);
                                    }
                                }
                                for (ItemStack stack : playerOffHandInv)
                                {
                                    if (TileEntityFurnace.getItemBurnTime(stack) > 0)
                                    {
                                        if (ModConfig.overheatOptions.incinerateitemstoash)
                                        {
                                            ItemStack insultingAsh = new ItemStack(WOOD_ASH, stack.getCount());
                                            insultingAsh.setStackDisplayName("\u00a7r\u00a78" + stack.getDisplayName() + " Ash");
                                            ashToSpawn.add(insultingAsh);
                                        }
                                        stack.setCount(0);
                                    }
                                }
                            }

                            //Check to see if we can place a fire below the player because daum look at that hot... block.
                            if (ModConfig.overheatOptions.incineratedPlayersStartFires && world.getBlockState(playerPos).getBlock().isReplaceable(world, playerPos))
                                world.setBlockState(playerPos, Blocks.FIRE.getDefaultState(), 11);

                            //Kill this genius trying to wear a suit of hot.
                            if (!player.world.isRemote)
                                player.attackEntityFrom(playerIncinerator, Integer.MAX_VALUE);

                            //Insult that fool
                            NBTTagCompound insultingAshNBT = new NBTTagCompound();
                            NBTTagString insultingAshLore = new NBTTagString("All that remains of " + playerDisplayName + "\u00A75\u00A7o... So hot they turned to nothing but ash!");
                            NBTTagList insultingAshList = new NBTTagList();

                            insultingAshList.appendTag(insultingAshLore);
                            insultingAshNBT.setTag("display", new NBTTagCompound());
                            insultingAshNBT.getCompoundTag("display").setTag("Name", new NBTTagString("\u00a78Ash of " + playerDisplayName));
                            insultingAshNBT.getCompoundTag("display").setTag("Lore", insultingAshList);

                            ItemStack insultingAsh = new ItemStack(WOOD_ASH, 1);
                            insultingAsh.setTagCompound(insultingAshNBT);

                            EntityItem insultingAshEntity = new EntityItem(world, player.posX, player.posY, player.posZ, insultingAsh);
                            insultingAshEntity.setDefaultPickupDelay();
                            insultingAshEntity.setEntityInvulnerable(true);

                            //make sure we only spawn items on the server and then spawn all of them we need.
                            if (!player.world.isRemote)
                            {
                                if (ModConfig.overheatOptions.incinerateitemstoash)
                                    for (ItemStack ashStack : ashToSpawn)
                                    {
                                        EntityItem ashEntity = new EntityItem(world, player.posX, player.posY, player.posZ, ashStack);
                                        ashEntity.setEntityInvulnerable(true);
                                        world.spawnEntity(ashEntity);
                                    }

                                world.spawnEntity(insultingAshEntity);
                            }
                        }
                        //If they aren't hot enough to incinerate :(, Burn them instead! (if they are hot enough)
                        else if (itemTemp >= ModConfig.overheatOptions.heatToCombustPlayer)
                        {
                            //Make with the hots.
                            player.setFire((int) (heatCapability.getTemperature() - ModConfig.overheatOptions.heatToIncineratePlayer));
                        }
                    }
                }
            }
        }
    }

    private void spawnYeetItem(World world, BlockPos pos, ItemStack item)
    {
        float extraX = world.rand.nextFloat() * 0.8F + 0.3F;
        float extraY = world.rand.nextFloat() * 0.8F + 0.3F;
        float extraZ = world.rand.nextFloat() * 0.8F + 0.3F;
        int stackSize = item.getCount();
        ArrayList<ItemStack> stacksToSpawn = new ArrayList<>();

        while (stackSize > 0)
        {
            int shrinkBy = world.rand.nextInt(5);
            if (stackSize > shrinkBy)
            {
                stacksToSpawn.add(item.splitStack(shrinkBy));
                stackSize = stackSize - shrinkBy;
            }
            else
            {
                stacksToSpawn.add(item.splitStack(stackSize));
                stackSize = 0;
            }
        }

        for (ItemStack stack : stacksToSpawn)
        {
            // the item to be spawned and thrown
            EntityItem entityitem = new EntityItem(world, pos.getX() + (double) extraX, pos.getY() + (double) extraY, pos.getZ() + (double) extraZ, stack);
            //set a delay so the player doesn't instantly collect it if they are in the way
            entityitem.setPickupDelay(30);
            //Set the motion on the item
            entityitem.motionX = world.rand.nextGaussian() * 0.27D;
            entityitem.motionY = world.rand.nextGaussian() * 0.27D + 0.20000000298023224D;
            entityitem.motionZ = world.rand.nextGaussian() * 0.27D;
            // Spawn the entity with the constructed Entityitem.
            world.spawnEntity(entityitem);
        }
    }
}
