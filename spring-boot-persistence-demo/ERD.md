# Dromeas courier delivery: entity relationship diagram

Target schema of `spring-boot-persistence-demo` (Oracle 23ai, schema `DROMEAS`), as created by
`src/main/resources/db/migration/V1__create_delivery_schema.sql`.

## Keys and indexes only

The tables reduced to primary keys, foreign keys and indexed columns. Mermaid has no index marker, so each column's comment names the
index: `ix_*` is a plain index, `uq_*` a unique constraint (backed by a unique index). Every primary key has an implicit index (`pk_*`).

```mermaid
erDiagram
	depot ||--o{ courier : "fk_courier_depot"
	depot ||--o{ customer : "fk_customer_depot"
	depot ||--o{ shipment : "fk_shipment_origin_depot"
	depot ||--o{ shipment : "fk_shipment_destination_depot"
	depot ||--o{ tracking_event : "fk_tracking_event_depot"
	customer ||--o{ invoice : "fk_invoice_customer"
	customer ||--o{ shipment : "fk_shipment_customer"
	invoice |o--o{ shipment : "fk_shipment_invoice"
	courier |o--o{ shipment : "fk_shipment_courier"
	shipment ||--o{ tracking_event : "fk_tracking_event_shipment"

	depot {
		NUMBER id PK "pk_depot"
		VARCHAR2 code UK "uq_depot_code"
	}

	courier {
		NUMBER id PK "pk_courier"
		NUMBER depot_id FK "fk_courier_depot, ix_courier_depot"
		VARCHAR2 employee_code UK "uq_courier_employee_code"
	}

	customer {
		NUMBER id PK "pk_customer"
		NUMBER depot_id FK "fk_customer_depot, ix_customer_depot"
		VARCHAR2 customer_code UK "uq_customer_code"
		VARCHAR2 email UK "uq_customer_email"
		VARCHAR2 vat_number UK "uq_customer_vat_number"
	}

	invoice {
		NUMBER id PK "pk_invoice"
		NUMBER customer_id FK "fk_invoice_customer, uq_invoice_customer_period (customer_id, period_start)"
		DATE period_start UK "uq_invoice_customer_period (customer_id, period_start)"
		VARCHAR2 invoice_number UK "uq_invoice_number"
	}

	shipment {
		NUMBER id PK "pk_shipment"
		NUMBER customer_id FK "fk_shipment_customer, ix_shipment_customer"
		NUMBER origin_depot_id FK "fk_shipment_origin_depot, ix_shipment_origin_depot"
		NUMBER destination_depot_id FK "fk_shipment_destination_depot, ix_shipment_destination_depot"
		NUMBER courier_id FK "fk_shipment_courier, ix_shipment_courier"
		NUMBER invoice_id FK "fk_shipment_invoice, ix_shipment_invoice"
		VARCHAR2 tracking_number UK "uq_shipment_tracking_number"
		VARCHAR2 status "ix_shipment_status_created_at (status, created_at)"
		TIMESTAMP_TZ created_at "ix_shipment_status_created_at (status, created_at)"
	}

	tracking_event {
		NUMBER id PK "pk_tracking_event"
		NUMBER shipment_id FK "fk_tracking_event_shipment, ix_tracking_event_shipment_time (shipment_id, occurred_at)"
		NUMBER depot_id FK "fk_tracking_event_depot, ix_tracking_event_depot"
		TIMESTAMP_TZ occurred_at "ix_tracking_event_shipment_time (shipment_id, occurred_at)"
	}
```

## Full schema, all columns

```mermaid
erDiagram
    depot ||--o{ courier: "employs"
    depot ||--o{ customer: "serves pickups for"
    depot ||--o{ shipment: "origin"
    depot ||--o{ shipment: "destination"
    depot ||--o{ tracking_event: "scan location"
    customer ||--o{ invoice: "is billed"
    customer ||--o{ shipment: "sends"
    invoice |o--o{ shipment: "covers"
    courier |o--o{ shipment: "delivers"
    shipment ||--o{ tracking_event: "has"

    depot {
        NUMBER id PK "identity, number(19)"
        VARCHAR2 code UK "varchar2(10), e.g. ATH"
        VARCHAR2 name "varchar2(120)"
        VARCHAR2 depot_type "varchar2(10), HUB or LOCAL"
        VARCHAR2 city "varchar2(120)"
        VARCHAR2 region "varchar2(120)"
        VARCHAR2 address_line "varchar2(200)"
        VARCHAR2 postal_code "varchar2(20)"
        DATE opened_on
        TIMESTAMP_TZ created_at "default systimestamp"
        TIMESTAMP_TZ updated_at "default systimestamp"
        NUMBER version "number(19), default 0"
    }

    courier {
        NUMBER id PK "identity, number(19)"
        NUMBER depot_id FK "number(19)"
        VARCHAR2 employee_code UK "varchar2(20), e.g. CR-0001"
        VARCHAR2 first_name "varchar2(100)"
        VARCHAR2 last_name "varchar2(100)"
        VARCHAR2 phone "varchar2(30)"
        VARCHAR2 vehicle_type "varchar2(20), MOTORBIKE, CARGO_BIKE, VAN or TRUCK"
        NUMBER daily_capacity "number(4), > 0"
        NUMBER active "number(1), 0 or 1, default 1"
        DATE hired_on
        TIMESTAMP_TZ created_at "default systimestamp"
        TIMESTAMP_TZ updated_at "default systimestamp"
        NUMBER version "number(19), default 0"
    }

    customer {
        NUMBER id PK "identity, number(19)"
        VARCHAR2 customer_code UK "varchar2(20), e.g. CU-00001"
        VARCHAR2 customer_type "varchar2(10), PRIVATE or BUSINESS"
        VARCHAR2 first_name "varchar2(100), nullable, required for PRIVATE"
        VARCHAR2 last_name "varchar2(100), nullable, required for PRIVATE"
        VARCHAR2 company_name "varchar2(200), nullable, required for BUSINESS"
        VARCHAR2 vat_number UK "varchar2(9), nullable, required for BUSINESS"
        VARCHAR2 contract_tier "varchar2(10), STANDARD, SILVER or GOLD, default STANDARD"
        VARCHAR2 email UK "varchar2(320)"
        VARCHAR2 phone "varchar2(30)"
        NUMBER depot_id FK "number(19)"
        VARCHAR2 line1 "varchar2(200)"
        VARCHAR2 city "varchar2(120)"
        VARCHAR2 postal_code "varchar2(20)"
        VARCHAR2 country "varchar2(2), default GR"
        TIMESTAMP_TZ created_at "default systimestamp"
        TIMESTAMP_TZ updated_at "default systimestamp"
        NUMBER version "number(19), default 0"
    }

    invoice {
        NUMBER id PK "identity, number(19)"
        VARCHAR2 invoice_number UK "varchar2(30), e.g. INV-202603-00012"
        NUMBER customer_id FK "number(19), unique with period_start"
        DATE period_start "unique with customer_id"
        DATE period_end ">= period_start"
        DATE issued_on
        DATE due_on
        NUMBER net_amount "number(12,2)"
        NUMBER vat_amount "number(12,2)"
        NUMBER total_amount "number(12,2)"
        VARCHAR2 status "varchar2(10), ISSUED, PAID or OVERDUE"
        DATE paid_on "nullable, set only when PAID"
        TIMESTAMP_TZ created_at "default systimestamp"
        TIMESTAMP_TZ updated_at "default systimestamp"
        NUMBER version "number(19), default 0"
    }

    shipment {
        NUMBER id PK "identity, number(19)"
        VARCHAR2 tracking_number UK "varchar2(12), DR + 10 digits"
        NUMBER customer_id FK "number(19), sender"
        NUMBER origin_depot_id FK "number(19)"
        NUMBER destination_depot_id FK "number(19)"
        NUMBER courier_id FK "number(19), nullable until dispatched"
        NUMBER invoice_id FK "number(19), nullable until invoiced"
        VARCHAR2 service_level "varchar2(10), STANDARD, EXPRESS or SAME_DAY"
        VARCHAR2 status "varchar2(20), default CREATED"
        NUMBER weight_kg "number(7,2), > 0"
        NUMBER price "number(10,2), >= 0"
        VARCHAR2 recipient_name "varchar2(200)"
        VARCHAR2 recipient_line1 "varchar2(200)"
        VARCHAR2 recipient_city "varchar2(120)"
        VARCHAR2 recipient_postal_code "varchar2(20)"
        TIMESTAMP_TZ picked_up_at "nullable"
        TIMESTAMP_TZ promised_by
        TIMESTAMP_TZ delivered_at "nullable"
        NUMBER attempts "number(2), default 0"
        TIMESTAMP_TZ created_at "default systimestamp"
        TIMESTAMP_TZ updated_at "default systimestamp"
        NUMBER version "number(19), default 0"
    }

    tracking_event {
        NUMBER id PK "identity, number(19)"
        NUMBER shipment_id FK "number(19)"
        NUMBER depot_id FK "number(19)"
        VARCHAR2 status "varchar2(20), PICKED_UP, AT_HUB, OUT_FOR_DELIVERY, DELIVERED or FAILED_ATTEMPT"
        TIMESTAMP_TZ occurred_at
        VARCHAR2 note "varchar2(200), nullable"
        TIMESTAMP_TZ created_at "default systimestamp"
    }
```

## Notes

- `shipment.status` allows `CREATED`, `PICKED_UP`, `AT_HUB`, `OUT_FOR_DELIVERY`, `DELIVERED` and `FAILED_ATTEMPT`.
- `tracking_event` is an append-only log: no `updated_at`, no `version`.
- `customer` carries a check constraint tying the identity columns to `customer_type`: a `PRIVATE` customer has a first and last name
  and no company or VAT number; a `BUSINESS` customer has a company name and a VAT number.
- Foreign-key columns are indexed (`ix_*`), plus `ix_shipment_status_created_at` on `(status, created_at)` and
  `ix_tracking_event_shipment_time` on `(shipment_id, occurred_at)`.
