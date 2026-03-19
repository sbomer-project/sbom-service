-- GENERATION HANDLER OPTIONS
create table generation_handler_options (
                                            generation_db_id bigint not null,
                                            opt_key varchar(255) not null,
                                            opt_value varchar(255),
                                            primary key (generation_db_id, opt_key)
);

-- FOREIGN KEYS
alter table if exists generation_handler_options
    add constraint FK_gen_hndlr_opt_gen
    foreign key (generation_db_id) references generations;
