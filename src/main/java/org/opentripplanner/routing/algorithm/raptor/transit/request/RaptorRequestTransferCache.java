package org.opentripplanner.routing.algorithm.raptor.transit.request;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import org.opentripplanner.routing.algorithm.raptor.transit.RaptorTransferIndex;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.BicycleOptimizeType;

public class RaptorRequestTransferCache {

    private final LoadingCache<CacheKey, RaptorTransferIndex> transferCache;

    public RaptorRequestTransferCache(int maximumSize) {
        transferCache = CacheBuilder.newBuilder()
            .maximumSize(maximumSize)
            .build(cacheLoader());
    }

    @SneakyThrows
    public RaptorTransferIndex get(
        List<List<Transfer>> transfersByStopIndex,
        RoutingRequest routingRequest
    ) {
        return transferCache.get(new CacheKey(
            transfersByStopIndex,
            routingRequest
        ));
    }

    private CacheLoader<CacheKey, RaptorTransferIndex> cacheLoader() {
        return new CacheLoader<>() {
            @Override
            public RaptorTransferIndex load(@javax.annotation.Nonnull CacheKey cacheKey) {
                return RaptorTransferIndex.create(
                        cacheKey.transfersByStopIndex,
                        cacheKey.routingRequest
                );
            }
        };
    }

    private static class CacheKey {

        private final List<List<Transfer>> transfersByStopIndex;
        private final RoutingRequest routingRequest;
        private final StreetRelevantOptions options;

        private CacheKey(
                List<List<Transfer>> transfersByStopIndex,
                RoutingRequest routingRequest
        ) {
            this.transfersByStopIndex = transfersByStopIndex;
            this.routingRequest = routingRequest;
            this.options = new StreetRelevantOptions(routingRequest);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            CacheKey cacheKey = (CacheKey) o;
            // transfersByStopIndex is checked using == on purpose since the instance should not change
            // (there is only one instance per graph)
            return transfersByStopIndex == cacheKey.transfersByStopIndex
                && options.equals(cacheKey.options);
        }

        @Override
        public int hashCode() {
            // transfersByStopIndex is ignored on purpose since it should not change (there is only
            // one instance per graph) and calculating the hashCode() would be expensive
            return options.hashCode();
        }
    }

    /**
     * This contains an extract of the parameters which may influence transfers. The possible values
     * are somewhat limited by rounding in {@link Transfer#prepareTransferRoutingRequest(RoutingRequest)}.
     *
     * TODO: the bikeWalking options are not used.
     */
    @EqualsAndHashCode
    private static class StreetRelevantOptions {

        private final StreetMode transferMode;
        private final BicycleOptimizeType optimize;
        private final double bikeTriangleSafetyFactor;
        private final double bikeTriangleSlopeFactor;
        private final double bikeTriangleTimeFactor;
        private final boolean wheelchairAccessible;
        private final double maxWheelchairSlope;
        private final double walkSpeed;
        private final double bikeSpeed;
        private final double walkReluctance;
        private final double stairsReluctance;
        private final double turnReluctance;
        private final int elevatorBoardCost;
        private final int elevatorBoardTime;
        private final int elevatorHopCost;
        private final int elevatorHopTime;
        private final int bikeSwitchCost;
        private final int bikeSwitchTime;

        public StreetRelevantOptions(RoutingRequest routingRequest) {
            this.transferMode = routingRequest.modes.transferMode;

            this.optimize = routingRequest.bicycleOptimizeType;
            this.bikeTriangleSafetyFactor = routingRequest.bikeTriangleSafetyFactor;
            this.bikeTriangleSlopeFactor = routingRequest.bikeTriangleSlopeFactor;
            this.bikeTriangleTimeFactor = routingRequest.bikeTriangleTimeFactor;
            this.bikeSwitchCost = routingRequest.bikeSwitchCost;
            this.bikeSwitchTime = routingRequest.bikeSwitchTime;

            this.wheelchairAccessible = routingRequest.wheelchairAccessible;
            this.maxWheelchairSlope = routingRequest.maxWheelchairSlope;

            this.walkSpeed = routingRequest.walkSpeed;
            this.bikeSpeed = routingRequest.bikeSpeed;

            this.walkReluctance = routingRequest.walkReluctance;
            this.stairsReluctance = routingRequest.stairsReluctance;
            this.turnReluctance = routingRequest.turnReluctance;

            this.elevatorBoardCost = routingRequest.elevatorBoardCost;
            this.elevatorBoardTime = routingRequest.elevatorBoardTime;
            this.elevatorHopCost = routingRequest.elevatorHopCost;
            this.elevatorHopTime = routingRequest.elevatorHopTime;
        }
    }
}
