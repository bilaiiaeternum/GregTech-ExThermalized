package mods.gregtechmod.init;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ic2.api.item.IC2Items;
import ic2.api.recipe.Recipes;
import ic2.core.init.OreValues;
import ic2.core.util.StackUtil;
import mods.gregtechmod.api.GregTechAPI;
import mods.gregtechmod.api.recipe.CellType;
import mods.gregtechmod.api.util.Reference;
import mods.gregtechmod.compat.ModHandler;
import mods.gregtechmod.core.GregTechConfig;
import mods.gregtechmod.core.GregTechMod;
import mods.gregtechmod.objects.BlockItems;
import mods.gregtechmod.recipe.*;
import mods.gregtechmod.recipe.crafting.ToolCraftingRecipeShaped;
import mods.gregtechmod.recipe.crafting.ToolOreIngredient;
import mods.gregtechmod.recipe.ingredient.RecipeIngredientFluid;
import mods.gregtechmod.recipe.ingredient.RecipeIngredientItemStack;
import mods.gregtechmod.recipe.ingredient.RecipeIngredientOre;
import mods.gregtechmod.util.*;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.OreIngredient;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class OreDictHandler {
    public static final OreDictHandler INSTANCE;

    private static final Pattern GRANITE_PATTERN = Pattern.compile("\\bstone.*Granite");
    private static final Map<String, String> GT_ORE_NAMES;
    private static final Map<String, Integer> VALUABLE_ORES;
    private static final Map<String, Pair<Supplier<Item>, Supplier<Item>>> BLAST_FURNACE_DUSTS = new HashMap<>();
    private static final Map<String, Supplier<ItemStack>> DUST_TO_INGOT = new HashMap<>();
    private static final Map<String, Supplier<ItemStack>> ORE_TO_INGOT = new HashMap<>();
    private static final List<String> IGNORED_NAMES;

    private boolean activated = false;

    static {
        Path path = GtUtil.getAssetPath("oredict.json");
        JsonObject json = JsonHandler.readJSON(path);
        GT_ORE_NAMES = StreamEx.of(json.getAsJsonObject("ore_aliases").entrySet())
            .toMap(Map.Entry::getKey, entry -> entry.getValue().getAsString());

        VALUABLE_ORES = StreamEx.of(json.getAsJsonObject("valuable_ores").entrySet())
            .toMap(Map.Entry::getKey, entry -> entry.getValue().getAsInt());

        IGNORED_NAMES = StreamEx.of(json.getAsJsonArray("ignored_names").iterator())
            .map(JsonElement::getAsString)
            .toList();

        BLAST_FURNACE_DUSTS.put("steel", Pair.of(BlockItems.Ingot.STEEL::getInstance, BlockItems.Dust.STEEL::getInstance));
        BLAST_FURNACE_DUSTS.put("chrome", Pair.of(BlockItems.Ingot.CHROME::getInstance, BlockItems.Dust.CHROME::getInstance));
        BLAST_FURNACE_DUSTS.put("titanium", Pair.of(BlockItems.Ingot.TITANIUM::getInstance, BlockItems.Dust.TITANIUM::getInstance));
        Pair<Supplier<Item>, Supplier<Item>> tungsten = Pair.of(BlockItems.Ingot.TUNGSTEN::getInstance, BlockItems.Dust.TUNGSTEN::getInstance);
        BLAST_FURNACE_DUSTS.put("tungsten", tungsten);
        BLAST_FURNACE_DUSTS.put("tungstate", tungsten);
        Pair<Supplier<Item>, Supplier<Item>> aluminium = Pair.of(BlockItems.Ingot.ALUMINIUM::getInstance, BlockItems.Dust.ALUMINIUM::getInstance);
        BLAST_FURNACE_DUSTS.put("aluminium", aluminium);
        BLAST_FURNACE_DUSTS.put("aluminum", aluminium);

        DUST_TO_INGOT.put("invar", BlockItems.Ingot.INVAR::getItemStack);
        DUST_TO_INGOT.put("brass", BlockItems.Ingot.BRASS::getItemStack);
        DUST_TO_INGOT.put("nickel", BlockItems.Ingot.NICKEL::getItemStack);
        DUST_TO_INGOT.put("antimony", BlockItems.Ingot.ANTIMONY::getItemStack);
        DUST_TO_INGOT.put("zinc", BlockItems.Ingot.ZINC::getItemStack);
        DUST_TO_INGOT.put("platinum", BlockItems.Ingot.PLATINUM::getItemStack);
        DUST_TO_INGOT.put("lead", BlockItems.Ingot.LEAD::getItemStack);
        DUST_TO_INGOT.put("silver", BlockItems.Ingot.SILVER::getItemStack);
        DUST_TO_INGOT.put("electrum", BlockItems.Ingot.ELECTRUM::getItemStack);
        DUST_TO_INGOT.put("copper", () -> IC2Items.getItem("ingot", "copper"));
        DUST_TO_INGOT.put("bronze", () -> IC2Items.getItem("ingot", "bronze"));
        DUST_TO_INGOT.put("tin", () -> IC2Items.getItem("ingot", "tin"));

        ORE_TO_INGOT.put("silver", BlockItems.Ingot.SILVER::getItemStack);
        ORE_TO_INGOT.put("lead", BlockItems.Ingot.LEAD::getItemStack);
        ORE_TO_INGOT.put("copper", () -> IC2Items.getItem("ingot", "copper"));
        ORE_TO_INGOT.put("tin", () -> IC2Items.getItem("ingot", "tin"));
        ORE_TO_INGOT.put("zinc", BlockItems.Ingot.ZINC::getItemStack);
        ORE_TO_INGOT.put("antimony", BlockItems.Ingot.ANTIMONY::getItemStack);
        ORE_TO_INGOT.put("cassiterite", () -> StackUtil.setSize(IC2Items.getItem("ingot", "tin"), 2));
        ORE_TO_INGOT.put("cooperite", BlockItems.Ingot.PLATINUM::getItemStack);
        ORE_TO_INGOT.put("sheldonite", BlockItems.Ingot.PLATINUM::getItemStack);
        ORE_TO_INGOT.put("platinum", BlockItems.Ingot.PLATINUM::getItemStack);
        ORE_TO_INGOT.put("nickel", BlockItems.Ingot.NICKEL::getItemStack);
        ORE_TO_INGOT.put("electrum", BlockItems.Ingot.ELECTRUM::getItemStack);

        INSTANCE = new OreDictHandler();
    }

    private OreDictHandler() {}

    public static void registerValuableOres() {
        VALUABLE_ORES.forEach((key, value) -> OreDictionary.getOres(key)
            .forEach(stack -> OreValues.add(stack, value)));
    }

    @Nullable
    public static String getOreAlias(String ore) {
        return GT_ORE_NAMES.get(ore);
    }

    public static void addOreAlias(String ore, String alias) {
        GT_ORE_NAMES.put(ore, alias);
    }

    public static void removeOreAlias(String ore) {
        GT_ORE_NAMES.remove(ore);
    }

    public static void addValuableOre(String ore, int value) {
        VALUABLE_ORES.put(ore, value);
    }

    public static void removeValuableOre(String ore) {
        VALUABLE_ORES.remove(ore);
    }

    public static void addIgnoredName(String ore) {
        IGNORED_NAMES.add(ore);
    }

    public static void removeIgnoredName(String ore) {
        IGNORED_NAMES.remove(ore);
    }

    @SubscribeEvent
    public void registerOre(OreDictionary.OreRegisterEvent event) {
        if (event == null || !this.activated) return;

        registerOre(event.getName(), event.getOre());
    }

    public void registerOre(String name, ItemStack ore) {
        if (ore.isEmpty() || name.isEmpty() || IGNORED_NAMES.contains(name) || name.toLowerCase(Locale.ROOT).contains("xych") || name.toLowerCase(Locale.ROOT).contains("xyore")) return;

        if (ore.getCount() != 1) GregTechMod.LOGGER.error("'" + name + "' is either being misused by another mod or has been wrongly registered, as the stack size of the event stack is not 1");
        ore.setCount(1);

        String unifiedName = GT_ORE_NAMES.get(name);
        if (unifiedName != null) OreDictUnificator.registerOre(unifiedName, ore);

        if (name.startsWith("denseOre")) {
            OreDictUnificator.registerOre(name.replaceFirst("denseOre", "oreDense"), ore);
            return;
        }
        else if (name.startsWith("netherOre")) {
            OreDictUnificator.registerOre(name.replaceFirst("netherOre", "oreNether"), ore);
            return;
        }
        else if (name.startsWith("endOre")) {
            OreDictUnificator.registerOre(name.replaceFirst("endOre", "oreEnd"), ore);
            return;
        }
        else if (name.startsWith("itemDrop")) {
            OreDictUnificator.registerOre(name.replaceFirst("itemDrop", "item"), ore);
            return;
        }
        else if (GRANITE_PATTERN.matcher(name).find()) OreDictUnificator.registerOre("stoneGranite", ore);

        if (name.startsWith("plate") || name.startsWith("ore") || name.startsWith("dust") || name.startsWith("gem")
            || name.startsWith("ingot") || name.startsWith("nugget") || name.startsWith("block") || name.startsWith("stick")) OreDictUnificator.addAssociation(name, ore.copy());

        registerRecipes(name, ore);
    }

    public void activateHandler() {
        this.activated = true;
        Arrays.stream(OreDictionary.getOreNames())
            .forEach(name -> OreDictionary.getOres(name)
                .forEach(ore -> registerOre(name, ore)));
    }

    public void registerRecipes(String rawName, ItemStack ore) {
        if (ore.isEmpty()) return;

        ore.setCount(1);
        if (rawName.startsWith("plate") || rawName.startsWith("ore") || rawName.startsWith("dust") || rawName.startsWith("gem") || rawName.startsWith("ingot") || rawName.startsWith("nugget") || rawName.startsWith("block") || rawName.startsWith("stick")) OreDictUnificator.add(rawName, ore.copy());

        String name = rawName.startsWith("drop") ? rawName.replaceFirst("drop", "item") : rawName;

        if (name.startsWith("stone")) {
            processStone(ore, name);
        }
        else if (name.startsWith("ore")) {
            processOre(ore, name);
        }
        else if (name.startsWith("dust")) {
            processDust(ore, name);
        }
        else if (name.startsWith("ingot")) {
            processIngot(ore, name);
        }
        else if (name.startsWith("block")) {
            ItemStack unified = OreDictUnificator.getUnifiedOre(name);
            processBlock(unified, name);
        }
        else if (name.startsWith("nugget")) {
            ItemStack unified = OreDictUnificator.getUnifiedOre(name, ore);
            Recipes.recyclerBlacklist.add(Recipes.inputFactory.forOreDict(name));

            String ingotName = name.replaceFirst("nugget", "ingot");
            OreDictUnificator.getFirstOre(ingotName)
                .ifPresent(ingot -> {
                    if (!name.equals("nuggetIridium") && !name.equals("nuggetOsmium") && !name.equals("nuggetUranium") && !name.equals("nuggetPlutonium") && !name.equals("nuggetThorium")) {
                        DynamicRecipes.addAlloySmelterRecipe(RecipeAlloySmelter.create(Collections.singletonList(RecipeIngredientOre.create(name, 9)), ingot.copy(), 200, 1, false));
                    }
                    Ingredient input = new OreIngredient(name);
                    ModHandler.addShapelessRecipe(
                        name + "ToIngot",
                        ingot.copy(),
                        input, input, input, input, input, input, input, input, input
                    );
                    ModHandler.addShapelessRecipe(
                        ingotName + "ToNuggets",
                        ItemHandlerHelper.copyStackWithSize(unified, 9),
                        new OreIngredient(ingotName)
                    );
                    ModHandler.removeFactorizerRecipe(ingot, true);
                    ModHandler.removeFactorizerRecipe(unified, false);
                    ModHandler.addFactorizerRecipe(ingot, ItemHandlerHelper.copyStackWithSize(unified, 9), true);
                    ModHandler.addFactorizerRecipe(ItemHandlerHelper.copyStackWithSize(unified, 9), ingot, false);
                });
        }
        else if (name.startsWith("plate")) {
            if (!name.startsWith("plateAlloy")) {
                ModHandler.removeCraftingRecipe(ore);

                if (name.startsWith("plateDense")) {
                    OreDictUnificator.getFirstOre(name.replaceFirst("plateDense", "ingot"), 8)
                        .ifPresent(ingot -> DynamicRecipes.addSmeltingAndAlloySmeltingRecipe(name, ore, ingot));
                    OreDictUnificator.getFirstOre(name.replaceFirst("plateDense", "dust"), 8)
                        .ifPresent(dust -> DynamicRecipes.addPulverizerRecipe(RecipePulverizer.create(RecipeIngredientOre.create(name), dust)));
                }
                else {
                    OreDictUnificator.getFirstOre(name.replaceFirst("plate", "plateDense"))
                        .ifPresent(plateDense -> DynamicRecipes.addCompressorRecipe(Recipes.inputFactory.forOreDict(name, 8), plateDense));

                    OreDictUnificator.getFirstOre(name.replaceFirst("plate", "ingot"))
                        .ifPresent(ingot -> DynamicRecipes.addSmeltingAndAlloySmeltingRecipe(name, ore, ingot));

                    OreDictUnificator.getFirstOre(name.replaceFirst("plate", "dust"))
                        .ifPresent(dust -> DynamicRecipes.addPulverizerRecipe(RecipePulverizer.create(RecipeIngredientOre.create(name), dust)));
                }
            }
            String ingotName = name.replaceFirst("plate", "ingot");
            if (OreDictUnificator.oreExists(ingotName)) {
                ModHandler.addShapedRecipe(
                    name + "FromIngots",
                    ore,
                    "H", "I", "I", 'H', "craftingToolHardHammer", 'I', ingotName
                );
            }
        }
        else if (name.startsWith("paper") || name.startsWith("book") || name.equals("treeLeaves")) {
            OreDictUnificator.addAssociation(name, ore);
        }
        else if (name.equals("woodRubber") || name.equals("logRubber")) {
            DynamicRecipes.addSawmillRecipe(RecipeSawmill.create(RecipeIngredientOre.create(name), Arrays.asList(IC2Items.getItem("misc_resource", "resin"), BlockItems.Dust.WOOD.getItemStack(16)), 1, true));
        }
        else if (name.startsWith("log")) {
            processWithMeta(ore, name, this::processLog);
        }
        else if (name.startsWith("slabWood")) {
            processWithMeta(ore, name, this::processSlab);
        }
        else if (name.startsWith("plankWood")) {
            processWithMeta(ore, name, this::processPlank);

            DynamicRecipes.addLatheRecipe(RecipeLathe.create(RecipeIngredientOre.create(name), Collections.singletonList(new ItemStack(Items.STICK, 2)), 25, 8));
            DynamicRecipes.addAssemblerRecipe(RecipeDualInput.create(Arrays.asList(RecipeIngredientOre.create(name, 8), RecipeIngredientOre.create("dustRedstone")), new ItemStack(Blocks.NOTEBLOCK), 800, 1));
            DynamicRecipes.addAssemblerRecipe(RecipeDualInput.create(Arrays.asList(RecipeIngredientOre.create(name, 8), RecipeIngredientOre.create("gemDiamond")), new ItemStack(Blocks.JUKEBOX), 1600, 1));
        }
        else if (name.startsWith("treeSapling")) {
            processWithMeta(ore, name, this::processSapling);
        }
        else if (name.startsWith("stick")) {
            OreDictUnificator.addAssociation(name, ore);

            OreDictUnificator.getFirstOre(name.replaceFirst("stick", "dustSmall"), 2)
                .ifPresent(smallDust -> DynamicRecipes.addPulverizerRecipe(RecipePulverizer.create(RecipeIngredientOre.create(name), smallDust)));
        }
        else if (name.startsWith("seed")) {
            DynamicRecipes.addCentrifugeRecipe(RecipeCentrifuge.create(RecipeIngredientOre.create(name, 64), Collections.singletonList(ProfileDelegate.getCell("seed.oil")), 1, 200, CellType.CELL));
        }
        else if (name.startsWith("plant") || name.startsWith("flower")) {
            if (GregTechMod.classic) {
                DynamicRecipes.COMPRESSOR.addRecipe(Recipes.inputFactory.forOreDict(name, 8), IC2Items.getItem("crafting", "compressed_plants"));
            }
            else if (name.startsWith("plant")) {
                ModHandler.addShapedRecipe(
                    name + "ToPlantBall",
                    IC2Items.getItem("crafting", "plant_ball"),
                    "XXX", "X X", "XXX", 'X', name
                );
            }

            ModHandler.getRecipeOutput(ore)
                .ifPresent(dye -> DynamicRecipes.addExtractorRecipe(Recipes.inputFactory.forOreDict(name), ItemHandlerHelper.copyStackWithSize(dye, dye.getCount() + 1)));
        }
    }

    private void processWithMeta(ItemStack ore, String name, BiConsumer<ItemStack, String> processor) {
        if (ore.getMetadata() == OreDictionary.WILDCARD_VALUE) {
            Item item = ore.getItem();
            for (int i = 0; i < 16; i++) processor.accept(new ItemStack(item, 1, i), name);
        }
        else processor.accept(ore, name);
    }

    private void processSapling(ItemStack stack, String name) {
        OreDictUnificator.addAssociation(name, stack);
        DynamicRecipes.addPulverizerRecipe(RecipePulverizer.create(RecipeIngredientOre.create(name), BlockItems.Dust.WOOD.getItemStack(2)));
    }

    private void processPlank(ItemStack stack, String name) {
        OreDictUnificator.addAssociation(name, stack);
        ModHandler.removeTEPulverizerRecipe(stack);
        DynamicRecipes.addPulverizerRecipe(RecipePulverizer.create(RecipeIngredientOre.create(name), BlockItems.Dust.WOOD.getItemStack()));
    }

    private void processSlab(ItemStack stack, String name) {
        OreDictUnificator.addAssociation(name, stack);

        DynamicRecipes.addPulverizerRecipe(RecipePulverizer.create(RecipeIngredientOre.create(name), BlockItems.Smalldust.WOOD.getItemStack(2)));
        if (!ModHandler.woodenTie.isEmpty()) {
            Fluid creosote = FluidRegistry.getFluid("creosote");
            if (creosote != null) {
                DynamicRecipes.addCannerRecipe(RecipeCanner.create(RecipeIngredientOre.create(name, 3), RecipeIngredientFluid.fromFluid(creosote, 1), Collections.singletonList(ModHandler.woodenTie), 200, 4));
            }
        }
    }

    private void processLog(ItemStack stack, String name) {
        OreDictUnificator.addAssociation(name, stack);

        IRecipe recipe = ModHandler.getCraftingRecipe(stack);
        if (recipe != null) {
            ItemStack result = recipe.getRecipeOutput();
            if (!result.isEmpty()) {
                ItemStack planks = ItemHandlerHelper.copyStackWithSize(result, result.getCount() * 3 / 2);
                DynamicRecipes.addSawmillRecipe(RecipeSawmill.create(RecipeIngredientItemStack.create(stack), Arrays.asList(planks, BlockItems.Dust.WOOD.getItemStack()), 1, true));
                ModHandler.removeCraftingRecipeFromInputs(stack);
                String recipeName = recipe.getRegistryName().getPath();

                IRecipe sawingRecipe = ToolCraftingRecipeShaped.makeSawingRecipe(
                    result,
                    "S", "L", 'S', ToolOreIngredient.saw(), 'L', stack
                ).setRegistryName(new ResourceLocation(Reference.MODID, recipeName + "_sawing"));
                ForgeRegistries.RECIPES.register(sawingRecipe);

                GameRegistry.addShapelessRecipe(
                    new ResourceLocation(Reference.MODID, recipeName),
                    null,
                    ItemHandlerHelper.copyStackWithSize(result, result.getCount() / (GregTechConfig.GENERAL.woodNeedsSawForCrafting ? 2 : 1)),
                    Ingredient.fromStacks(stack)
                );
            }
        }
    }

    private void processStone(ItemStack stack, String name) {
        Item item = stack.getItem();
        if (item instanceof ItemBlock) GregTechAPI.instance().addJackHammerMinableBlock(stack);

        if (name.equals("stoneObsidian") && item instanceof ItemBlock) {
            ((ItemBlock) item).getBlock().setResistance(20);
        }
    }

    private void processBlock(ItemStack ore, String name) {
        String ingotName = name.replaceFirst("block", "ingot");
        OptionalItemStack ingot = OreDictUnificator.getFirstOre(ingotName);
        String gemName = name.replaceFirst("block", "gem");
        OptionalItemStack gem = gemName.equals("gemIridium") ? OptionalItemStack.EMPTY : OreDictUnificator.getFirstOre(gemName);
        String dustName = name.replaceFirst("block", "dust");
        OptionalItemStack dust = OreDictUnificator.getFirstOre(dustName);

        ModHandler.removeCraftingRecipeFromInputs(ore);
        if (!ingot.isPresent() && !gem.isPresent() && !dust.isPresent()) return;

        boolean storageBlockCrafting = GregTechAPI.getDynamicConfig("storage_block_crafting", name, false);
        boolean storageBlockDeCrafting = GregTechAPI.getDynamicConfig("storage_block_decrafting", name, gem.isPresent());

        boolean ingotPresent = ingot.ifPresent(stack -> {
            ModHandler.removeCraftingRecipeFromInputs(stack, stack, stack, stack, stack, stack, stack, stack, stack);

            stack.setCount(9);
            DynamicRecipes.addSmeltingAndAlloySmeltingRecipe(name, ore, stack);
            DynamicRecipes.addIngotToBlockRecipe("Ingots", ingotName, 9, name, stack, ore, storageBlockCrafting, storageBlockDeCrafting);
        });
        boolean gemPresent = gem.ifPresent(stack -> {
            if (ModHandler.getRecipeOutput(stack, stack, ItemStack.EMPTY, stack, stack).itemEquals(ore)) return;

            ModHandler.removeCraftingRecipeFromInputs(stack, stack, stack, stack, stack, stack, stack, stack, stack);

            stack.setCount(9);
            DynamicRecipes.addIngotToBlockRecipe("Gems", gemName, 9, name, stack, ore, storageBlockCrafting, storageBlockDeCrafting);
        });
        dust.ifPresent(stack -> {
            ModHandler.removeCraftingRecipeFromInputs(stack, stack, stack, stack, stack, stack, stack, stack, stack);

            int count = OreDictUnificator.isItemInstanceOf(stack, "dustGlowstone", false) ? 4 : 9;
            stack.setCount(count);
            if (!ingotPresent && !gemPresent) DynamicRecipes.addIngotToBlockRecipe("Dusts", dustName, count, name, stack, ore, storageBlockCrafting, storageBlockDeCrafting);
            DynamicRecipes.addPulverizerRecipe(RecipePulverizer.create(RecipeIngredientOre.create(name), stack));
        });
    }

    private void processIngot(final ItemStack stack, String name) {
        String material = name.replace("ingot", "");
        String materialName = material.toLowerCase(Locale.ROOT);
        String plateName = name.replaceFirst("ingot", "plate");
        ItemStack plate = OreDictUnificator.get(plateName);
        if (!plate.isEmpty()) {
            DynamicRecipes.addBenderRecipe(RecipeSimple.create(RecipeIngredientOre.create(name), plate, 50, 20));

            if (GregTechAPI.getDynamicConfig("plates_needed_for_armor_made_of", material, true)) {
                ModHandler.removeCraftingRecipeFromInputs(stack, stack, stack, stack, ItemStack.EMPTY, stack)
                    .ifPresent(output -> addShapedMaterialRecipe(materialName, "helmet", output, "XXX", "XTX", 'X', plateName, 'T', "craftingToolHardHammer"));

                ModHandler.removeCraftingRecipeFromInputs(stack, ItemStack.EMPTY, stack, stack, stack, stack, stack, stack, stack)
                    .ifPresent(output -> addShapedMaterialRecipe(materialName, "chestplate", output, "XTX", "XXX", "XXX", 'X', plateName, 'T', "craftingToolHardHammer"));

                ModHandler.removeCraftingRecipeFromInputs(stack, stack, stack, stack, ItemStack.EMPTY, stack, stack, ItemStack.EMPTY, stack)
                    .ifPresent(output -> addShapedMaterialRecipe(materialName, "leggings", output, "XXX", "XTX", "X X", 'X', plateName, 'T', "craftingToolHardHammer"));

                ModHandler.removeCraftingRecipeFromInputs(stack, ItemStack.EMPTY, stack, stack, ItemStack.EMPTY, stack)
                    .ifPresent(output -> addShapedMaterialRecipe(materialName, "boots", output, "XTX", "X X", 'X', plateName, 'T', "craftingToolHardHammer"));
            }

            if (GregTechAPI.getDynamicConfig("plates_needed_for_tools_made_of", name.replace("ingot", ""), true)) {
                ItemStack stick = new ItemStack(Items.STICK);
                ModHandler.removeCraftingRecipeFromInputs(ItemStack.EMPTY, stack, ItemStack.EMPTY, ItemStack.EMPTY, stack, ItemStack.EMPTY, ItemStack.EMPTY, stick)
                    .ifPresent(output -> addShapedMaterialRecipe(materialName, "sword", output, " X ", "FXT", " S ", 'X', plateName, 'T', "craftingToolHardHammer", 'S', "stickWood", 'F', "craftingToolFile"));

                ModHandler.removeCraftingRecipeFromInputs(stack, stack, stack, ItemStack.EMPTY, stick, ItemStack.EMPTY, ItemStack.EMPTY, stick)
                    .ifPresent(output -> addShapedMaterialRecipe(materialName, "pickaxe", output, "XII", "FST", " S ", 'X', plateName, 'T', "craftingToolHardHammer", 'S', "stickWood", 'F', "craftingToolFile", 'I', stack));

                ModHandler.removeCraftingRecipeFromInputs(ItemStack.EMPTY, stack, ItemStack.EMPTY, ItemStack.EMPTY, stick, ItemStack.EMPTY, ItemStack.EMPTY, stick)
                    .ifPresent(output -> addShapedMaterialRecipe(materialName, "shovel", output, "FXT", " S ", " S ", 'X', plateName, 'T', "craftingToolHardHammer", 'S', "stickWood", 'F', "craftingToolFile"));

                OptionalItemStack.when(
                    ModHandler.removeCraftingRecipeFromInputs(stack, stack, ItemStack.EMPTY, stack, stick, ItemStack.EMPTY, ItemStack.EMPTY, stick),
                    ModHandler.removeCraftingRecipeFromInputs(ItemStack.EMPTY, stack, stack, ItemStack.EMPTY, stick, stack, ItemStack.EMPTY, stick)
                ).ifPresent(output -> addShapedMaterialRecipe(materialName, "axe", output, "XIT", "XS ", "FS ", 'X', plateName, 'T', "craftingToolHardHammer", 'S', "stickWood", 'F', "craftingToolFile", 'I', stack));

                OptionalItemStack.when(
                    ModHandler.removeCraftingRecipeFromInputs(stack, stack, ItemStack.EMPTY, ItemStack.EMPTY, stick, ItemStack.EMPTY, ItemStack.EMPTY, stick),
                    ModHandler.removeCraftingRecipeFromInputs(ItemStack.EMPTY, stack, stack, ItemStack.EMPTY, stick, ItemStack.EMPTY, ItemStack.EMPTY, stick)
                ).ifPresent(output -> addShapedMaterialRecipe(materialName, "hoe", output, "XIT", "FS ", " S ", 'X', plateName, 'T', "craftingToolHardHammer", 'S', "stickWood", 'F', "craftingToolFile", 'I', stack));
            }
        }

        ItemStack unified = OreDictUnificator.getUnifiedOre(name, stack);

        if (!OreDictUnificator.getFirstOre("gearStone")
            .flatMap(gearStone -> ModHandler.getRecipeOutput(ItemStack.EMPTY, unified, ItemStack.EMPTY, unified, gearStone, unified, ItemStack.EMPTY, unified, ItemStack.EMPTY))
            .ifPresent(output -> DynamicRecipes.addAssemblerRecipe(RecipeDualInput.create(RecipeIngredientOre.create("gearStone"), RecipeIngredientItemStack.create(unified, 4), output, 1600, 2)))
        ) {
            OptionalItemStack.when(
                ModHandler.getRecipeOutput(ItemStack.EMPTY, unified, ItemStack.EMPTY, unified, new ItemStack(Items.IRON_INGOT), unified, ItemStack.EMPTY, unified, ItemStack.EMPTY),
                ModHandler.getRecipeOutput(ItemStack.EMPTY, unified, ItemStack.EMPTY, unified, new ItemStack(Blocks.COBBLESTONE), unified, ItemStack.EMPTY, unified, ItemStack.EMPTY)
            ).ifPresent(output -> DynamicRecipes.addAssemblerRecipe(RecipeDualInput.create(RecipeIngredientOre.create("stoneCobble"), RecipeIngredientItemStack.create(unified, 4), output, 1600, 2)));
        }

        switch (name) {
            case "ingotQuicksilver":
                DynamicRecipes.addDustToIngotSmeltingRecipe(name, BlockItems.Dust.CINNABAR.getItemStack(), unified);
                break;
            case "ingotManganese":
                DynamicRecipes.addDustToIngotSmeltingRecipe(name, BlockItems.Dust.MANGANESE.getItemStack(), unified);
                break;
            case "ingotMagnesium":
                DynamicRecipes.addDustToIngotSmeltingRecipe(name, BlockItems.Dust.MAGNESIUM.getItemStack(), unified);
                break;
            case "ingotAluminum":
            case "ingotTitanium":
            case "ingotChrome":
            case "ingotTungsten":
            case "ingotSteel":
                ModHandler.removeSmeltingRecipe(unified);
                break;
            case "ingotIron":
                if (GregTechMod.classic) DynamicRecipes.addInductionSmelterRecipe("ingotRefinedIron", unified, new ItemStack(Blocks.SAND), StackUtil.setSize(IC2Items.getItem("ingot", "refined_iron"), 2), ModHandler.slag, 400, 25);
                break;
        }

        OreDictUnificator.getFirstOre(name.replaceFirst("ingot", "stick"))
            .ifPresent(stick -> {
                ModHandler.addShapedRecipe(
                    "stick_" + materialName,
                    stick,
                    "F", "I", 'F', "craftingToolFile", 'I', name
                );

                BlockItems.Smalldust gtSmallDust = JavaUtil.getEnumConstantSafely(BlockItems.Smalldust.class, materialName.toUpperCase(Locale.ROOT));
                OptionalItemStack.when(
                    OptionalItemStack.of(gtSmallDust != null ? gtSmallDust.getItemStack() : ItemStack.EMPTY),
                    OreDictUnificator.getFirstOre(name.replaceFirst("ingot", "dustSmall"), 2)
                )
                    .ifEmpty(()-> DynamicRecipes.addLatheRecipe(RecipeLathe.create(RecipeIngredientOre.create(name), Arrays.asList(stick, stick), 150, 16)))
                    .ifPresent(smallDust -> DynamicRecipes.addLatheRecipe(RecipeLathe.create(RecipeIngredientOre.create(name), Arrays.asList(stick, smallDust), 50, 16)));
            });
    }

    private void addShapedMaterialRecipe(String material, String name, ItemStack result, Object... params) {
        ModHandler.addShapedRecipe(
            result.getItem().getRegistryName().getNamespace() + "_" + name + "_" + material,
            result,
            params
        );
    }

    private void processSmalldustRecipe(ItemStack stack, String name, String smalldustName, int count) {
        Recipes.recyclerBlacklist.add(Recipes.inputFactory.forOreDict(name));
        String dustName = name.replaceFirst(smalldustName, "dust");
        OreDictUnificator.getFirstOre(dustName)
            .ifPresent(dust -> {
                Ingredient[] ingredients = new Ingredient[count];
                Arrays.fill(ingredients, new OreIngredient(name));
                ModHandler.addShapelessRecipe(dustName + "FromSmalldusts", dust, ingredients);
                ModHandler.addShapelessRecipe(dustName + "ToSmalldusts", ItemHandlerHelper.copyStackWithSize(stack, count), new OreIngredient(dustName));
            });
    }

    private void processDust(ItemStack stack, String name) {
        if (name.startsWith("dustSmall")) processSmalldustRecipe(stack, name, "dustSmall", 4);
        if (name.startsWith("dustTiny")) processSmalldustRecipe(stack, name, "dustTiny", 9);

        String material = name.substring(4).toLowerCase(Locale.ROOT);
        if (BLAST_FURNACE_DUSTS.containsKey(material)) {
            if (GregTechAPI.getDynamicConfig("blast_furnace_requirements", material, true)) ModHandler.removeSmeltingRecipe(stack);
            else DynamicRecipes.addDustToIngotSmeltingRecipe(name, stack, BLAST_FURNACE_DUSTS.get(material).getKey().get());
        }
        else if (DUST_TO_INGOT.containsKey(material)) {
            DynamicRecipes.addDustToIngotSmeltingRecipe(name, stack, DUST_TO_INGOT.get(material).get());
        }

        switch (name) {
            case "dustCoal":
                ModHandler.addLiquidTransposerFillRecipe(stack, new FluidStack(FluidRegistry.WATER, 125), IC2Items.getItem("dust", "coal_fuel"), 1250);
                break;
            case "dustWheat":
            case "dustFlour":
                DynamicRecipes.addSmeltingRecipe(name, stack, new ItemStack(Items.BREAD));
                break;
        }
    }

    private void processOre(ItemStack stack, String name) {
        OreDictUnificator.getFirstOre(name.replaceFirst("ore", "dust"))
            .ifPresent(dust -> ModHandler.addShapedRecipe(
                name + "Crushing",
                dust,
                "T", "O", 'T', "craftingToolHardHammer", 'O', name));

        String material = name.substring(3).toLowerCase(Locale.ROOT);
        if (BLAST_FURNACE_DUSTS.containsKey(material)) {
            Pair<Supplier<Item>, Supplier<Item>> pair = BLAST_FURNACE_DUSTS.get(material);
            Item item = GregTechAPI.getDynamicConfig("blast_furnace_requirements", material, true) ? pair.getKey().get() : pair.getValue().get();
            DynamicRecipes.addSmelterOreToIngotsRecipe(stack, item);
        }
        else if (ORE_TO_INGOT.containsKey(material)) {
            DynamicRecipes.addSmelterOreToIngotsRecipe(stack, ORE_TO_INGOT.get(material).get());
        }

        switch (name) {
            case "oreIron":
            case "orePyrite":
                DynamicRecipes.addInductionSmelterRecipe(name, stack, new ItemStack(Blocks.SAND), new ItemStack(Items.IRON_INGOT), ModHandler.slag, 3000, 10);
                DynamicRecipes.addInductionSmelterRecipe(name, stack, ModHandler.slagRich, new ItemStack(Items.IRON_INGOT, 2), ModHandler.slag, 3000, 95);
                break;
            case "orePhosphorite":
                DynamicRecipes.addSmeltingRecipe(name, stack, BlockItems.Dust.PHOSPHORUS.getItemStack(2));
                break;
            case "oreSaltpeter":
                DynamicRecipes.addSmeltingRecipe(name, stack, BlockItems.Dust.SALTPETER.getItemStack(3));
                break;
            case "oreSulfur":
                DynamicRecipes.addSmeltingRecipe(name, stack, ItemHandlerHelper.copyStackWithSize(IC2Items.getItem("dust", "sulfur"), 3));
                break;
        }
    }
}
