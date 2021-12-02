package net.buycraft.plugin;

import com.google.gson.annotations.SerializedName;
import net.buycraft.plugin.data.Package;

import java.util.Date;

/**
 * SpaceDelta
 *
 * Represents a customer purchase
 * https://docs.tebex.io/plugin/endpoints/customer-purchases
 */
public class Purchase {

    @SerializedName("tnx_id")
    private final String transactionId;
    private final Date date;
    private final int quantity;
    @SerializedName("package")
    private final Package aPackage;

    public Purchase(String transactionId, Date date, int quantity, Package aPackage) {
        this.transactionId = transactionId;
        this.date = date;
        this.quantity = quantity;
        this.aPackage = aPackage;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public Date getDate() {
        return date;
    }

    public int getQuantity() {
        return quantity;
    }

    public Package getPackage() {
        return aPackage;
    }

}
