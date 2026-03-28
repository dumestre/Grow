-- SQL para criar tabela plants no Supabase
-- Execute este SQL no Editor SQL do Supabase
-- IMPORTANTE: Crie primeiro a tabela mural_users

-- 1. Criar tabela plants
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

-- 2. Habilitar RLS
ALTER TABLE public.plants ENABLE ROW LEVEL SECURITY;

-- 3. Políticas RLS (sem autenticação - usar mural_users)
CREATE POLICY "plants_select" ON public.plants
    FOR SELECT USING (true);

CREATE POLICY "plants_insert" ON public.plants
    FOR INSERT WITH CHECK (true);

CREATE POLICY "plants_update" ON public.plants
    FOR UPDATE USING (true);

CREATE POLICY "plants_delete" ON public.plants
    FOR DELETE USING (true);

-- 4. Criar índice para performance
CREATE INDEX IF NOT EXISTS idx_plants_user_id ON public.plants(user_id);
CREATE INDEX IF NOT EXISTS idx_plants_deleted_at ON public.plants(deleted_at) WHERE deleted_at IS NULL;
