-- PARKING 24 SEVEN - MIGRACION ADITIVA E IDEMPOTENTE
-- EJECUTAR EN MYSQL WORKBENCH SOLO DESPUES DE REALIZAR RESPALDO.
-- NO EJECUTAR parking24seven.sql (respaldo antiguo con DROP TABLE).
USE parking24seven;

-- Columnas conservadas si ya existen. MySQL 8.0 (sin ADD COLUMN IF NOT EXISTS).
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='usuarios' AND column_name='activo')=0,
 'ALTER TABLE usuarios ADD COLUMN activo TINYINT(1) NOT NULL DEFAULT 1', 'SELECT ''activo ya existe''');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='usuarios' AND column_name='rol')=0,
 'ALTER TABLE usuarios ADD COLUMN rol VARCHAR(20) NOT NULL DEFAULT ''OPERADOR''', 'SELECT ''rol ya existe''');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='usuarios' AND column_name='email')=0,
 'ALTER TABLE usuarios ADD COLUMN email VARCHAR(254) NULL', 'SELECT ''email ya existe''');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='usuarios' AND column_name='activado')=0,
 'ALTER TABLE usuarios ADD COLUMN activado TINYINT(1) NOT NULL DEFAULT 1', 'SELECT ''activado ya existe''');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='usuarios' AND column_name='version_sesion')=0,
 'ALTER TABLE usuarios ADD COLUMN version_sesion BIGINT NOT NULL DEFAULT 0', 'SELECT ''version_sesion ya existe''');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='usuarios' AND column_name='token_activacion_hash')=0,
 'ALTER TABLE usuarios ADD COLUMN token_activacion_hash CHAR(64) NULL', 'SELECT ''token_activacion_hash ya existe''');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name='usuarios' AND column_name='token_activacion_expira')=0,
 'ALTER TABLE usuarios ADD COLUMN token_activacion_expira DATETIME NULL', 'SELECT ''token_activacion_expira ya existe''');
PREPARE p FROM @ddl; EXECUTE p; DEALLOCATE PREPARE p;

-- La cuenta id=1 se mantiene protegida y con rol de administrador.
UPDATE usuarios SET rol = 'ADMINISTRADOR' WHERE id = 1;

CREATE TABLE IF NOT EXISTS auditoria_usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id INT NULL,
    objetivo_id INT NULL,
    accion VARCHAR(40) NOT NULL,
    detalle VARCHAR(500) NULL,
    INDEX idx_auditoria_fecha (fecha),
    INDEX idx_auditoria_objetivo (objetivo_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SELECT id, nomusu, activo, rol, activado, version_sesion FROM usuarios ORDER BY id;
SELECT COUNT(*) AS eventos_auditoria FROM auditoria_usuarios;
