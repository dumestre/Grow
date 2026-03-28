-- =============================================
-- SQL para criar TODAS as tabelas no Supabase
-- Execute este SQL no Editor SQL do Supabase
-- IMPORTANTE: Execute em ordem - primeiro excluindo tabelas existentes se necessário
-- =============================================

-- 1. Criar tabela mural_users
CREATE TABLE IF NOT EXISTS public.mural_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS')
);

-- 2. Criar tabela mural_posts
CREATE TABLE IF NOT EXISTS public.mural_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.mural_users(id) ON DELETE CASCADE,
    plant_name TEXT NOT NULL,
    strain TEXT,
    stage TEXT,
    medium TEXT,
    days INTEGER,
    photo_url TEXT,
    created_at TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS')
);

-- 3. Criar tabela mural_comments
CREATE TABLE IF NOT EXISTS public.mural_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.mural_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.mural_users(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES public.mural_comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS')
);

-- 4. Criar tabela mural_likes
CREATE TABLE IF NOT EXISTS public.mural_likes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.mural_posts(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.mural_users(id) ON DELETE CASCADE,
    created_at TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS'),
    UNIQUE(post_id, user_id)
);

-- 5. Criar tabela plants
CREATE TABLE IF NOT EXISTS public.plants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.mural_users(id),
    name TEXT NOT NULL,
    strain TEXT,
    stage TEXT NOT NULL DEFAULT 'Germinação',
    medium TEXT,
    days INTEGER NOT NULL DEFAULT 0,
    photo_url TEXT,
    next_watering_date BIGINT,
    sort_order INTEGER DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    is_hydroponic BOOLEAN DEFAULT false,
    deleted_at BIGINT
);

-- 6. Criar tabela app_config para Remote Config
CREATE TABLE IF NOT EXISTS public.app_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key TEXT NOT NULL UNIQUE,
    value_bool BOOLEAN DEFAULT false,
    value_text TEXT,
    created_at TEXT NOT NULL DEFAULT to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS')
);

-- Inserir config inicial
INSERT INTO public.app_config (key, value_bool) VALUES ('use_alternative_icons', false)
ON CONFLICT (key) DO NOTHING;

-- =============================================
-- HABILITAR RLS EM TODAS AS TABELAS
-- =============================================

ALTER TABLE public.mural_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mural_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mural_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mural_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;

-- =============================================
-- POLÍTICAS RLS - mural_users
-- =============================================

DROP POLICY IF EXISTS "mural_users_select" ON public.mural_users;
CREATE POLICY "mural_users_select" ON public.mural_users
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "mural_users_insert" ON public.mural_users;
CREATE POLICY "mural_users_insert" ON public.mural_users
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "mural_users_update" ON public.mural_users;
CREATE POLICY "mural_users_update" ON public.mural_users
    FOR UPDATE USING (true);

DROP POLICY IF EXISTS "mural_users_delete" ON public.mural_users;
CREATE POLICY "mural_users_delete" ON public.mural_users
    FOR DELETE USING (true);

-- =============================================
-- POLÍTICAS RLS - mural_posts
-- =============================================

DROP POLICY IF EXISTS "mural_posts_select" ON public.mural_posts;
CREATE POLICY "mural_posts_select" ON public.mural_posts
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "mural_posts_insert" ON public.mural_posts;
CREATE POLICY "mural_posts_insert" ON public.mural_posts
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "mural_posts_update" ON public.mural_posts;
CREATE POLICY "mural_posts_update" ON public.mural_posts
    FOR UPDATE USING (true);

DROP POLICY IF EXISTS "mural_posts_delete" ON public.mural_posts;
CREATE POLICY "mural_posts_delete" ON public.mural_posts
    FOR DELETE USING (true);

-- =============================================
-- POLÍTICAS RLS - mural_comments
-- =============================================

DROP POLICY IF EXISTS "mural_comments_select" ON public.mural_comments;
CREATE POLICY "mural_comments_select" ON public.mural_comments
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "mural_comments_insert" ON public.mural_comments;
CREATE POLICY "mural_comments_insert" ON public.mural_comments
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "mural_comments_update" ON public.mural_comments;
CREATE POLICY "mural_comments_update" ON public.mural_comments
    FOR UPDATE USING (true);

DROP POLICY IF EXISTS "mural_comments_delete" ON public.mural_comments;
CREATE POLICY "mural_comments_delete" ON public.mural_comments
    FOR DELETE USING (true);

-- =============================================
-- POLÍTICAS RLS - mural_likes
-- =============================================

DROP POLICY IF EXISTS "mural_likes_select" ON public.mural_likes;
CREATE POLICY "mural_likes_select" ON public.mural_likes
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "mural_likes_insert" ON public.mural_likes;
CREATE POLICY "mural_likes_insert" ON public.mural_likes
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "mural_likes_delete" ON public.mural_likes;
CREATE POLICY "mural_likes_delete" ON public.mural_likes
    FOR DELETE USING (true);

-- =============================================
-- POLÍTICAS RLS - plants
-- =============================================

DROP POLICY IF EXISTS "plants_select" ON public.plants;
CREATE POLICY "plants_select" ON public.plants
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "plants_insert" ON public.plants;
CREATE POLICY "plants_insert" ON public.plants
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "plants_update" ON public.plants;
CREATE POLICY "plants_update" ON public.plants
    FOR UPDATE USING (true);

DROP POLICY IF EXISTS "plants_delete" ON public.plants;
CREATE POLICY "plants_delete" ON public.plants
    FOR DELETE USING (true);

-- =============================================
-- POLÍTICAS RLS - app_config
-- =============================================

DROP POLICY IF EXISTS "app_config_select" ON public.app_config;
CREATE POLICY "app_config_select" ON public.app_config
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "app_config_insert" ON public.app_config;
CREATE POLICY "app_config_insert" ON public.app_config
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "app_config_update" ON public.app_config;
CREATE POLICY "app_config_update" ON public.app_config
    FOR UPDATE USING (true);

-- =============================================
-- ÍNDICES PARA PERFORMANCE
-- =============================================

CREATE INDEX IF NOT EXISTS idx_mural_posts_user_id ON public.mural_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_mural_comments_post_id ON public.mural_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_mural_comments_user_id ON public.mural_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_mural_likes_post_id ON public.mural_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_mural_likes_user_id ON public.mural_likes(user_id);
CREATE INDEX IF NOT EXISTS idx_plants_user_id ON public.plants(user_id);
CREATE INDEX IF NOT EXISTS idx_plants_deleted_at ON public.plants(deleted_at) WHERE deleted_at IS NULL;

-- =============================================
-- BUCKET PARA STORAGE DE FOTOS
-- =============================================

-- Execute no Supabase Dashboard > Storage > New Bucket
-- Bucket name: plant-photos
-- Public: true
