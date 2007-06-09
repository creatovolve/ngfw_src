-- events schema for release-5.0

CREATE TABLE events.n_openvpn_statistic_evt (
        event_id    INT8 NOT NULL,
        time_stamp  TIMESTAMP,
        rx_bytes    INT8,
        tx_bytes    INT8,
        start_time  TIMESTAMP,
        end_time    TIMESTAMP,
        PRIMARY KEY (event_id));

CREATE TABLE events.n_openvpn_distr_evt (
        event_id INT8 NOT NULL,
        remote_address INET,
        client_name TEXT,
        time_stamp  TIMESTAMP,
        PRIMARY KEY (event_id));

CREATE TABLE events.n_openvpn_connect_evt (
        event_id INT8 NOT NULL,
        remote_address INET,
        remote_port    INT4,
        client_name    TEXT,
        rx_bytes       INT8,
        tx_bytes       INT8,
        time_stamp     TIMESTAMP,
        start_time     TIMESTAMP,
        end_time       TIMESTAMP,
        PRIMARY KEY    (event_id));

