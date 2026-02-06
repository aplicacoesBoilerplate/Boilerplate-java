CREATE TABLE log_errors (
    id_error BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Unique error identifier',
    error_message VARCHAR(255) NOT NULL COMMENT 'Brief description of the error',
    error_file VARCHAR(150) COMMENT 'File where the error occurred',
    error_class VARCHAR(150) COMMENT 'Java class where the error occurred',
    error_method VARCHAR(150) COMMENT 'Java method where the error occurred',
    error_line INT COMMENT 'Line of code where the error occurred',
    error_date_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL  COMMENT 'Date and time of the error',
    error_status_code INT NOT NULL COMMENT 'HTTP status related to the error',
    CONSTRAINT pk_log_erros PRIMARY KEY (id_error)
) ENGINE=InnoDB, COMMENT='Application error persistence table';
