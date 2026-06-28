# Use the PostGIS image you successfully used earlier
FROM postgis/postgis:15-3.4

# Install the pgvector extension for PostgreSQL 15
RUN apt-get update \
    && apt-get install -y postgresql-15-pgvector \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*