package id.co.lolita.laundry.order.adapter.out.gateway;

import id.co.lolita.laundry.order.domain.port.out.DriverGateway;
import id.co.lolita.laundry.user.domain.port.in.UserDirectoryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class DriverGatewayAdapter implements DriverGateway {

    private final UserDirectoryQuery userDirectory;

    @Override
    public boolean isActiveDriver(Long userId) {
        return userDirectory.isActiveDriver(userId);
    }
}