CREATE TABLE IF NOT EXISTS `tb_account`
(
    `id`        INTEGER auto_increment,
    `user_name` VARCHAR(100),
    `age`       Integer,
    `sex`       Integer,
    `birthday`  DATETIME,
    `options`   VARCHAR(1024),
    `is_delete` Integer,
    `version`   Integer
    );
