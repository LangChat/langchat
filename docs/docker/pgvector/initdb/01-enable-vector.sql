-- ============================================================================
-- PGVector 初始化：启用向量扩展
--
-- 该脚本由官方镜像在数据目录为空时自动执行（/docker-entrypoint-initdb.d）。
-- pgvector 镜像是「PostgreSQL + pgvector 扩展」，扩展本身需显式 CREATE EXTENSION
-- 才能使用，否则应用建向量表时会报 "type vector does not exist"。
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- 验证扩展已就绪（构建日志中可见）
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
