package su.nightexpress.nexshop.shop.chest.impl;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.engine.api.config.JYML;
import su.nightexpress.nexshop.ExcellentShop;
import su.nightexpress.nexshop.Placeholders;
import su.nightexpress.nexshop.ShopAPI;
import su.nightexpress.nexshop.api.event.ShopPurchaseEvent;
import su.nightexpress.nexshop.api.shop.ProductStock;
import su.nightexpress.nexshop.api.type.StockType;
import su.nightexpress.nexshop.api.type.TradeType;
import su.nightexpress.nexshop.config.Lang;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ChestProductStock extends ProductStock<ChestProduct> {

    public ChestProductStock() {

    }

    @Override
    public void write(@NotNull JYML cfg, @NotNull String path) {

    }

    @Override
    @NotNull
    public UnaryOperator<String> replacePlaceholders() {
        ExcellentShop plugin = ShopAPI.PLUGIN;

        int stockGLBuyAmountLeft = this.getLeftAmount(TradeType.BUY);
        int stockGLSellAmountLeft = this.getLeftAmount(TradeType.SELL);
        String infin = plugin.getMessage(Lang.OTHER_INFINITY).getLocalized();

        return str -> str
            .replace(Placeholders.PRODUCT_STOCK_GLOBAL_BUY_AMOUNT_LEFT, stockGLBuyAmountLeft < 0 ? infin : String.valueOf(stockGLBuyAmountLeft))
            .replace(Placeholders.PRODUCT_STOCK_GLOBAL_SELL_AMOUNT_LEFT, stockGLSellAmountLeft < 0 ? infin : String.valueOf(stockGLSellAmountLeft))
            ;
    }

    @Override
    @NotNull
    public UnaryOperator<String> replacePlaceholders(@NotNull Player player) {
        return str -> str;
    }

    @NotNull
    private ChestShop getShop() {
        return this.getProduct().getShop();
    }

    @Override
    public void onPurchase(@NotNull ShopPurchaseEvent<?> event) {
        TradeType tradeType = event.getTradeType();
        int amount = event.getPrepared().getAmount();
        Player player = event.getPlayer();

        if (!this.isUnlimited(StockType.GLOBAL, tradeType)) {
            int amountLeft = this.getLeftAmount(tradeType);
            this.setLeftAmount(tradeType, amountLeft - amount);
        }
    }

    @Override
    public int getInitialAmount(@NotNull StockType stockType, @NotNull TradeType tradeType) {
        return -1;
    }

    @Override
    public void setInitialAmount(@NotNull StockType stockType, @NotNull TradeType tradeType, int amount) {

    }

    @Override
    public boolean isUnlimited(@NotNull StockType stockType, @NotNull TradeType tradeType) {
        return this.getShop().isAdminShop();
    }

    @Override
    public int getRestockCooldown(@NotNull StockType stockType, @NotNull TradeType tradeType) {
        return 0;
    }

    @Override
    public void setRestockCooldown(@NotNull StockType stockType, @NotNull TradeType tradeType, int cooldown) {

    }

    @Override
    public int getPossibleAmount(@NotNull TradeType tradeType, @Nullable Player player) {
        return this.getLeftAmount(tradeType);
    }

    @Override
    public int getLeftAmount(@NotNull TradeType tradeType, @Nullable Player player) {
        if (this.getShop().isAdminShop()) return -1;

        Inventory inventory = this.getShop().getInventory();

        // Для покупки со стороны игрока, возвращаем количество реальных предметов в контейнере.
        if (tradeType == TradeType.BUY) {
            return Stream.of(inventory.getContents()).filter(has -> has != null && this.getProduct().isItemMatches(has))
                .mapToInt(ItemStack::getAmount).sum();
        }
        // Для продажи со стороны игрока, возвращаем количество в свободных и идентичных стопках для предмета.
        else {
            ItemStack item = this.getProduct().getItem();
            int productSize = (int) Stream.of(inventory.getContents())
                .filter(itemHas -> itemHas == null || itemHas.getType().isAir() || this.getProduct().isItemMatches(itemHas)).count();
            int maxSpace = productSize * item.getMaxStackSize();

            return maxSpace - this.getLeftAmount(TradeType.BUY);
        }
    }

    @Override
    public void setLeftAmount(@NotNull TradeType tradeType, int amount, @Nullable Player player) {
        int amountHas = this.getLeftAmount(tradeType);
        if (tradeType == TradeType.BUY) {
            // Has: 10, Set: 20, Need to add 10 items
            // Has: 10, Set: 5, Need to remove 5 items
            amount = amount - amountHas;
        }
        else {
            // Has: 10 space, Set: 20, = -10 = Need to remove 10 items
            // Has: 10 space, Set: 5, = 5 = Need to add 5 items
            amount = amountHas - amount;
        }
        boolean isRemoval = amount < 0;

        ItemStack item = this.getProduct().getItem();
        item.setAmount(Math.abs(amount));
        Inventory inventory = this.getShop().getInventory();

        if (isRemoval) {
            inventory.removeItem(item);
        }
        else {
            inventory.addItem(item);
        }
    }

    @Override
    public long getRestockDate(@NotNull TradeType tradeType, @Nullable Player player) {
        return 0;
    }
}
