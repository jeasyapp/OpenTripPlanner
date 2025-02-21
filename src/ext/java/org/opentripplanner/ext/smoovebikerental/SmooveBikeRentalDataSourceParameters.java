package org.opentripplanner.ext.smoovebikerental;

import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class SmooveBikeRentalDataSourceParameters extends VehicleRentalDataSourceParameters {
    private final String network;

    public SmooveBikeRentalDataSourceParameters(
        String url,
        String network,
        @NotNull
        Map<String, String> httpHeaders
    ) {
        super(DataSourceType.SMOOVE, url, httpHeaders);
        this.network = network;
    }

    /**
     * Each updater can be assigned a unique network ID in the configuration to prevent
     * returning bikes at stations for another network.
     * TODO shouldn't we give each updater a unique network ID by default?
     */
    @Nullable
    public String getNetwork(String defaultValue) {
        return network == null || network.isEmpty() ? defaultValue : network;
    }
}