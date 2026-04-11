package sn.metapower.gymtrack.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.metapower.gymtrack.entities.TarifTicket;

import java.util.List;

public interface TarifTicketRepository extends JpaRepository<TarifTicket, Long> {
    List<TarifTicket> findByActifTrue();
}
