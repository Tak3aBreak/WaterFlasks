package org.labellum.mc.waterflasks.item;

/** Largely borrowed from TFC ItemJug
 *  EUPL license meshes with GPLv3
 */

import net.dries007.tfc.api.capability.food.FoodStatsTFC;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.dries007.tfc.common.capabilities.size.IItemSize;
import net.dries007.tfc.common.capabilities.size.Size;
import net.dries007.tfc.common.capabilities.size.Weight;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.common.items.DiscreteFluidContainerItem;
import net.dries007.tfc.objects.fluids.properties.FluidWrapper;
import net.dries007.tfc.util.Drinkable;
import net.dries007.tfc.util.FluidTransferHelper;

import net.dries007.tfc.util.Helpers;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.World;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.ItemFluidContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.labellum.mc.waterflasks.Waterflasks;
import org.labellum.mc.waterflasks.fluids.FlaskFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Collectors;

import static net.dries007.tfc.api.capability.food.IFoodStatsTFC.MAX_PLAYER_THIRST;
import static net.dries007.tfc.common.capabilities.food.TFCFoodData.MAX_THIRST;
import static org.labellum.mc.waterflasks.Waterflasks.FLASK_BREAK;
import static org.labellum.mc.waterflasks.Waterflasks.MOD_ID;
import static org.labellum.mc.waterflasks.setup.ModSetup.FLASK_BREAK;
import static org.labellum.mc.waterflasks.setup.Registration.brokenIronFlask;
import static org.labellum.mc.waterflasks.setup.Registration.brokenLeatherFlask;

public abstract class ItemFlask extends DiscreteFluidContainerItem implements IItemSize {

    private int CAPACITY;
    private int DRINK;
    //private int maxDamage; // implemented by registrar

    protected String name;

    public ItemFlask(Item.Properties prop, String name, int CAPACITY, int DRINK) {

        super(prop, CAPACITY, TFCTags.Fluids.USABLE_IN_JUG,false,false);
        this.CAPACITY=CAPACITY;
        this.DRINK=DRINK;
        this.name=name;
        setRegistryName(name);
        //setHasSubtypes(true);
    }

    // Fix #12 by actually implementing the MC function that limits stack sizes
    @Override
    public int getItemStackLimit(ItemStack stack) { return getWeight(stack).stackSize; }

    @Nonnull
    @Override
    public Size getSize(@Nonnull ItemStack stack) { return Size.SMALL; }

    @Nonnull
    @Override
    public Weight getWeight(@Nonnull ItemStack stack) { return Weight.MEDIUM; }

    @Override
    public ICapabilityProvider initCapabilities(@Nonnull ItemStack stack, @Nullable CompoundTag nbt)
    {
        return new FlaskFluidHandler(stack, CAPACITY, TFCFluids.FLUIDS.getEntries().stream().filter(x -> Drinkable.get(x.get()) != null).map(RegistryObject::get).collect(Collectors.toSet()));
    }

    public void registerItemModel() {
        initModel(this, 0, name);
    }

    @SideOnly(Side.CLIENT)
    public void initModel(Item item, int meta, String id) {
        ModelResourceLocation modelFull = new ModelResourceLocation(Waterflasks.MOD_ID + ":" + id , "inventory");
        ModelResourceLocation model4 = new ModelResourceLocation(Waterflasks.MOD_ID + ":" + id + "-4", "inventory");
        ModelResourceLocation model3 = new ModelResourceLocation(Waterflasks.MOD_ID + ":" + id + "-3", "inventory");
        ModelResourceLocation model2 = new ModelResourceLocation(Waterflasks.MOD_ID + ":" + id + "-2", "inventory");
        ModelResourceLocation model1 = new ModelResourceLocation(Waterflasks.MOD_ID + ":" + id + "-1", "inventory");
        ModelResourceLocation model0 = new ModelResourceLocation(Waterflasks.MOD_ID + ":" + id + "-0", "inventory");

        ModelBakery.registerItemVariants(this, modelFull, model4, model3, model2, model1, model0);

        ModelLoader.setCustomMeshDefinition(this, new ItemMeshDefinition() {
            @Override
            public ModelResourceLocation getModelLocation(ItemStack stack) {
                switch ((int) Math.floor(getLiquidAmount(stack)/(double)CAPACITY * 5F)) {
                    case 5:
                        return modelFull;
                    case 4:
                        return model4;
                    case 3:
                        return model3;
                    case 2:
                        return model2;
                    case 1:
                        return model1;
                    default:
                        return model0;
                }
            }
        });
    }

    public int getLiquidAmount(ItemStack stack) {
        int content = 0;
        LazyOptional<IFluidHandlerItem> flaskCap = stack.getCapability(Capabilities.FLUID_ITEM, null);
        if (flaskCap.isPresent()) {
            FluidStack drained = flaskCap.resolve().get().drain(CAPACITY, IFluidHandler.FluidAction.SIMULATE);
            content = drained.getAmount();
        }
        return content;
    }

    /**
     * Returns the packed int RGB value used to render the durability bar in the GUI.
     * Retrieves no-alpha RGB color from liquid to use in durability bar
     *
     * @param stack Stack to get color from
     * @return A packed RGB value for the durability colour (0x00RRGGBB)
     */
    @Override
    public int getBarColor(ItemStack stack)
    {
        LazyOptional<IFluidHandlerItem> flaskCap = stack.getCapability(Capabilities.FLUID_ITEM, null);
        if (flaskCap.isPresent()) {
            FluidStack drained = flaskCap.resolve().get().drain(CAPACITY, IFluidHandler.FluidAction.SIMULATE);
            Fluid fluid = drained.getFluid();
            return fluid.getAttributes().getColor();
        }
        return super.getBarColor(stack);
    }



    @SuppressWarnings("ConstantConditions")
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        final ItemStack stack = player.getItemInHand(hand);
        final IFluidHandler handler = Helpers.getCapability(stack, Capabilities.FLUID_ITEM);
        if (handler == null)
        {
            return InteractionResultHolder.pass(stack);
        }
        else
        {
            // Do not use in creative game mode
            if(player.isCreative())
                return InteractionResultHolder.pass(stack);

            // If contains fluid, allow emptying with shift-right-click
            if(player.isCrouching())
            {
                handler.drain(CAPACITY, IFluidHandler.FluidAction.EXECUTE);
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }

            final BlockHitResult hit = Helpers.rayTracePlayer(level, player, ClipContext.Fluid.SOURCE_ONLY);
            if (FluidHelpers.transferBetweenWorldAndItem(stack, level, hit, player, hand, false, false, false))
            {
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }

            if (handler.getFluidInTank(0).isEmpty())
            {
                return afterFillFailed(handler, level, player, stack, hand);
            }
            else
            {             //Try to Drink
                FoodData stats = player.getFoodData();
                if (stats instanceof TFCFoodData && ((TFCFoodData) stats).getThirst() >= MAX_THIRST) {
                    // Don't drink if not thirsty
                    return InteractionResultHolder.fail(player.getItemInHand(hand));
                }
                FluidStack cont = handler.drain(CAPACITY, IFluidHandler.FluidAction.SIMULATE);
                if (cont != null && cont.getAmount() >= DRINK) {
                    return afterEmptyFailed(handler, level, player, stack, hand);
                }
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    @Nonnull
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, Level level, LivingEntity entity)
    {
        final IFluidHandler handler = stack.getCapability(Capabilities.FLUID_ITEM).resolve().orElse(null);
        if (handler != null)
        {
            final FluidStack drained = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (drained.getAmount() > DRINK) {
                FluidStack fluidConsumed = handler.drain(DRINK, IFluidHandler.FluidAction.EXECUTE);
                final Player player = entity instanceof Player ? (Player) entity : null;
                if (player != null)
                {
                    final Drinkable drinkable = Drinkable.get(fluidConsumed.getFluid());
                    if (drinkable != null)
                    {
                        drinkable.onDrink(player, fluidConsumed.getAmount());
                    }
                }
                if (stack.getDamageValue() == stack.getMaxDamage()) {
                    ResourceLocation name = stack.getItem().getRegistryName();
                    //break item, play sound
                    level.playSound((Player) entity, entity.getOnPos(), FLASK_BREAK.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                    if (name.toString().contains("leather"))
                    {
                        ItemHandlerHelper.giveItemToPlayer((Player) entity, new ItemStack(brokenLeatherFlask.get()));
                    }
                    else
                    {
                        ItemHandlerHelper.giveItemToPlayer((Player) entity, new ItemStack(brokenIronFlask.get()));
                    }
                    stack.shrink(1); //race condition here, seems to only sometimes work if done before giving broken flask
                }
                else
                {
                    stack.setDamageValue(stack.getDamageValue() + 1);
                }
            }
        }
        return stack;
    }

    @NotNull
    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack)
    {
        return PotionItem.EAT_DURATION;
    }

    @NotNull
    @Override
    protected InteractionResultHolder<ItemStack> afterEmptyFailed(IFluidHandler handler, Level level, Player player, ItemStack stack, InteractionHand hand)
    {
        if (player.isCrouching())
        {
            level.playSound(player, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.5f, 1.2f);
            handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            return InteractionResultHolder.consume(stack);
        }
        final Drinkable drinkable = Drinkable.get(handler.getFluidInTank(0).getFluid());
        if (drinkable != null)
        {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }
        return InteractionResultHolder.pass(stack);
    }
}
