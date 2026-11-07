-- Sonora local database (Room / SQLite). Settings live in DataStore, see architecture.md.
-- Times are UTC epoch milliseconds (SQLite has no timestamptz).
-- Only the user's own Backdrops are stored; built-in Backdrops are gradients defined in code.

create table backdrops (
  id          text    primary key not null,          -- UUID string
  name        text    not null,
  source      text    not null check (source in ('album_art','upload')),
  image_path  text    not null,                      -- filesDir-relative
  sort_order  integer not null default 0,
  created_at  integer not null,
  updated_at  integer not null
);

create index backdrops_order on backdrops (sort_order, created_at);
