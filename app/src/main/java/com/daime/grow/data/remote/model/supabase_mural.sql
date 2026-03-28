-- =============================================
-- SQL para criar tabelas do Mural no Supabase
-- Execute este SQL no Editor SQL do Supabase
-- IMPORTANTE: Exclua as tabelas existentes antes de executar
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

-- 5. Habilitar RLS
ALTER TABLE public.mural_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mural_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mural_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mural_likes ENABLE ROW LEVEL SECURITY;

-- 6. Políticas RLS para mural_users
DROP POLICY IF EXISTS "Users can view all mural users" ON public.mural_users;
CREATE POLICY "Users can view all mural users" ON public.mural_users
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert their own user" ON public.mural_users;
CREATE POLICY "Users can insert their own user" ON public.mural_users
    FOR INSERT WITH CHECK (true);

-- 7. Políticas RLS para mural_posts
DROP POLICY IF EXISTS "Users can view all posts" ON public.mural_posts;
CREATE POLICY "Users can view all posts" ON public.mural_posts
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert their own posts" ON public.mural_posts;
CREATE POLICY "Users can insert their own posts" ON public.mural_posts
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Users can delete their own posts" ON public.mural_posts;
CREATE POLICY "Users can delete their own posts" ON public.mural_posts
    FOR DELETE USING (true);

-- 8. Políticas RLS para mural_comments
DROP POLICY IF EXISTS "Users can view all comments" ON public.mural_comments;
CREATE POLICY "Users can view all comments" ON public.mural_comments
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert their own comments" ON public.mural_comments;
CREATE POLICY "Users can insert their own comments" ON public.mural_comments
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Users can delete their own comments" ON public.mural_comments;
CREATE POLICY "Users can delete their own comments" ON public.mural_comments
    FOR DELETE USING (true);

DROP POLICY IF EXISTS "Users can update their own comments" ON public.mural_comments;
CREATE POLICY "Users can update their own comments" ON public.mural_comments
    FOR UPDATE USING (true);

-- 9. Políticas RLS para mural_likes
DROP POLICY IF EXISTS "Users can view all likes" ON public.mural_likes;
CREATE POLICY "Users can view all likes" ON public.mural_likes
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Users can insert their own likes" ON public.mural_likes;
CREATE POLICY "Users can insert their own likes" ON public.mural_likes
    FOR INSERT WITH CHECK (true);

DROP POLICY IF EXISTS "Users can delete their own likes" ON public.mural_likes;
CREATE POLICY "Users can delete their own likes" ON public.mural_likes
    FOR DELETE USING (true);

-- 10. Criar índices para performance
CREATE INDEX IF NOT EXISTS idx_mural_posts_user_id ON public.mural_posts(user_id);
CREATE INDEX IF NOT EXISTS idx_mural_comments_post_id ON public.mural_comments(post_id);
CREATE INDEX IF NOT EXISTS idx_mural_comments_user_id ON public.mural_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_mural_likes_post_id ON public.mural_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_mural_likes_user_id ON public.mural_likes(user_id);
