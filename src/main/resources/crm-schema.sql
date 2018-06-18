create table orders (
  id  int          auto_increment primary key not null,
  sku varchar(255) default null,
  primary key (id)
);
