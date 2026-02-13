package org.jboss.sbomer.sbom.service.adapter.out.persistence.domain.entity;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "request_publishers")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PublisherEntity extends PanacheEntityBase {

    // --- SURROGATE KEY ---
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "db_id")
    private Long dbId;
    @EqualsAndHashCode.Include
    private String name;
    @EqualsAndHashCode.Include
    private String version;

    @ElementCollection
    @CollectionTable(name = "publisher_options", joinColumns = @JoinColumn(name = "publisher_db_id"))
    @MapKeyColumn(name = "opt_key")
    @Column(name = "opt_value")
    private Map<String, String> options = new HashMap<>();
}
