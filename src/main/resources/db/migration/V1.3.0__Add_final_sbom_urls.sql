-- FINAL SBOM URLS
create table final_sbom_urls (
                                 generation_db_id bigint not null,
                                 url varchar(255)
);

-- FOREIGN KEY
alter table if exists final_sbom_urls
    add constraint FK_final_urls_gen
    foreign key (generation_db_id) references generations;
