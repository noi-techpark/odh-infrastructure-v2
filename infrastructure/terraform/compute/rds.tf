resource "random_password" "odh-postgres-db-password" {
    length = 20
    upper = true
    numeric = true
    special = false
}

resource "aws_db_subnet_group" "odh-postgres" {
    name = "odh-postgres"
    subnet_ids = module.vpc.public_subnets
}

resource "aws_security_group" "allow-postgres-noi" {
    name = "allow-postgres-noi"
    description = "allow postgres RDS access to users in NOI subnet"
    vpc_id = module.vpc.vpc_id
    
    ingress {
        description = "Postgres from NOI"
        to_port = 5432
        from_port = 0
        protocol = "tcp"
        cidr_blocks = ["46.18.27.34/32"]
    }
}
resource "aws_security_group" "allow-postgres-all" {
    name = "allow-postgres-all"
    description = "allow postgres RDS access to everyone"
    vpc_id = module.vpc.vpc_id
    
    ingress {
        description = "Postgres from Everywhere"
        to_port = 5432
        from_port = 0
        protocol = "tcp"
        cidr_blocks = ["0.0.0.0/0"]
    }
}

data "aws_security_group" "default" {
    name = "default"
    vpc_id = module.vpc.vpc_id
}

resource "aws_db_instance" "odh-postgres" {
    identifier = "odh-postgres"
    instance_class = "db.t4g.medium"
    allocated_storage = 20
    engine = "postgres"
    engine_version = "15.3"
    username = "postgres"
    password = random_password.odh-postgres-db-password.result
    db_subnet_group_name = aws_db_subnet_group.odh-postgres.name
    publicly_accessible = true
    skip_final_snapshot = true
    vpc_security_group_ids = [data.aws_security_group.default.id, aws_security_group.allow-postgres-noi.id, aws_security_group.allow-postgres-all.id]
}

provider "postgresql" {
    scheme = "awspostgres"
    alias = "odh-postgres"
    host = aws_db_instance.odh-postgres.address
    port = aws_db_instance.odh-postgres.port
    username = aws_db_instance.odh-postgres.username
    password = aws_db_instance.odh-postgres.password
    sslmode = "require"
    connect_timeout = 15
    superuser = false
}

resource "postgresql_database" "bdp" {
    provider = postgresql.odh-postgres
    name = "bdp"
}

resource "postgresql_extension" "postgis" {
    provider = postgresql.odh-postgres
    database = postgresql_database.bdp.name
    name = "postgis"
}

resource "random_password" "odh-postgres-db-bdp" {
    length = 20
    upper = true
    numeric = true
    special = false
}
resource "random_password" "odh-postgres-db-bdp-readonly" {
    length = 20
    upper = true
    numeric = true
    special = false
}

resource "postgresql_role" "bdp" {
    provider = postgresql.odh-postgres
    name = "bdp"
    login = "true"
    password = random_password.odh-postgres-db-bdp.result 
}

resource "postgresql_role" "bdp_readonly" {
    provider = postgresql.odh-postgres
    name = "bdp_readonly"
    login = "true"
    password = random_password.odh-postgres-db-bdp-readonly.result 
}

resource "postgresql_schema" "intimev2" {
    provider = postgresql.odh-postgres
    name = "intimev2"
    database = postgresql_database.bdp.name
    owner = postgresql_role.bdp.name
}

resource "postgresql_grant" "bdp_all" {
    provider = postgresql.odh-postgres
    database = postgresql_database.bdp.name
    role = postgresql_role.bdp.name
    schema = postgresql_schema.intimev2.name
    object_type = "schema"
    privileges = ["CREATE", "USAGE"]
}

resource "postgresql_grant" "bdp_readonly_schema" {
    provider = postgresql.odh-postgres
    database = postgresql_database.bdp.name
    role = postgresql_role.bdp_readonly.name
    schema = postgresql_schema.intimev2.name
    object_type = "schema"
    privileges = ["USAGE"]
}
resource "postgresql_grant" "bdp_readonly_tables" {
    provider = postgresql.odh-postgres
    database = postgresql_database.bdp.name
    role = postgresql_role.bdp_readonly.name
    schema = postgresql_schema.intimev2.name
    object_type = "table"
    objects = [] # empty means all
    privileges = ["SELECT"]
}
