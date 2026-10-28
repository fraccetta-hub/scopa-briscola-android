// ASUFinder — Supabase Database Types
// Manually derived from supabase/migrations/001_initial_schema.sql
// Re-generate with: npx supabase gen types typescript --local > src/lib/supabase/types.ts
// once a Supabase project is linked.

export type Json =
  | string
  | number
  | boolean
  | null
  | { [key: string]: Json | undefined }
  | Json[]

export type Database = {
  public: {
    Tables: {
      profiles: {
        Row: {
          id: string
          username: string
          role: 'user' | 'admin' | 'superadmin' | 'banned'
          points: number
          created_at: string
        }
        Insert: {
          id: string
          username: string
          role?: 'user' | 'admin' | 'superadmin' | 'banned'
          points?: number
          created_at?: string
        }
        Update: {
          id?: string
          username?: string
          role?: 'user' | 'admin' | 'superadmin' | 'banned'
          points?: number
          created_at?: string
        }
        Relationships: []
      }
      places: {
        Row: {
          id: string
          name: string
          categories: string[]
          tags: string[]
          barrio: string
          address: string | null
          lat: number | null
          lng: number | null
          phone: string | null
          whatsapp: string | null
          website: string | null
          maps_url: string | null
          hours: string | null
          photo_url: string | null
          status: 'pending' | 'live' | 'needs_review' | 'closed' | 'rejected'
          source: 'user' | 'osm'
          is_featured: boolean
          featured_until: string | null
          added_by: string | null
          created_at: string
          updated_at: string
        }
        Insert: {
          id?: string
          name: string
          categories?: string[]
          tags?: string[]
          barrio: string
          address?: string | null
          lat?: number | null
          lng?: number | null
          phone?: string | null
          whatsapp?: string | null
          website?: string | null
          maps_url?: string | null
          hours?: string | null
          photo_url?: string | null
          status?: 'pending' | 'live' | 'needs_review' | 'closed' | 'rejected'
          source?: 'user' | 'osm'
          is_featured?: boolean
          featured_until?: string | null
          added_by?: string | null
          created_at?: string
          updated_at?: string
        }
        Update: {
          id?: string
          name?: string
          categories?: string[]
          tags?: string[]
          barrio?: string
          address?: string | null
          lat?: number | null
          lng?: number | null
          phone?: string | null
          whatsapp?: string | null
          website?: string | null
          maps_url?: string | null
          hours?: string | null
          photo_url?: string | null
          status?: 'pending' | 'live' | 'needs_review' | 'closed' | 'rejected'
          source?: 'user' | 'osm'
          is_featured?: boolean
          featured_until?: string | null
          added_by?: string | null
          created_at?: string
          updated_at?: string
        }
        Relationships: []
      }
      confirmations: {
        Row: {
          id: string
          place_id: string
          user_id: string
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          user_id: string
          created_at?: string
        }
        Update: {
          id?: string
          place_id?: string
          user_id?: string
          created_at?: string
        }
        Relationships: []
      }
      flags: {
        Row: {
          id: string
          place_id: string
          user_id: string
          reason: string | null
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          user_id: string
          reason?: string | null
          created_at?: string
        }
        Update: {
          id?: string
          place_id?: string
          user_id?: string
          reason?: string | null
          created_at?: string
        }
        Relationships: []
      }
      place_products: {
        Row: {
          id: string
          place_id: string
          tag: string
          added_by: string | null
          status: 'pending' | 'confirmed' | 'uncertain' | 'expired' | 'denied'
          confirmed_count: number
          denied_count: number
          expires_at: string
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          tag: string
          added_by?: string | null
          status?: 'pending' | 'confirmed' | 'uncertain' | 'expired' | 'denied'
          confirmed_count?: number
          denied_count?: number
          expires_at?: string
          created_at?: string
        }
        Update: {
          id?: string
          place_id?: string
          tag?: string
          added_by?: string | null
          status?: 'pending' | 'confirmed' | 'uncertain' | 'expired' | 'denied'
          confirmed_count?: number
          denied_count?: number
          expires_at?: string
          created_at?: string
        }
        Relationships: []
      }
      product_votes: {
        Row: {
          id: string
          place_product_id: string
          user_id: string
          type: 'confirm' | 'deny'
          created_at: string
        }
        Insert: {
          id?: string
          place_product_id: string
          user_id: string
          type: 'confirm' | 'deny'
          created_at?: string
        }
        Update: {
          id?: string
          place_product_id?: string
          user_id?: string
          type?: 'confirm' | 'deny'
          created_at?: string
        }
        Relationships: []
      }
      tag_synonyms: {
        Row: {
          id: string
          tag_a: string
          tag_b: string
        }
        Insert: {
          id?: string
          tag_a: string
          tag_b: string
        }
        Update: {
          id?: string
          tag_a?: string
          tag_b?: string
        }
        Relationships: []
      }
      reviews: {
        Row: {
          id: string
          place_id: string
          user_id: string
          body: string
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          user_id: string
          body: string
          created_at?: string
        }
        Update: {
          id?: string
          place_id?: string
          user_id?: string
          body?: string
          created_at?: string
        }
        Relationships: []
      }
      place_owners: {
        Row: {
          id: string
          place_id: string
          user_id: string
          verified: boolean
          verified_at: string | null
          approved_by: string | null
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          user_id: string
          verified?: boolean
          verified_at?: string | null
          approved_by?: string | null
          created_at?: string
        }
        Update: {
          id?: string
          place_id?: string
          user_id?: string
          verified?: boolean
          verified_at?: string | null
          approved_by?: string | null
          created_at?: string
        }
        Relationships: []
      }
      otp_tokens: {
        Row: {
          id: string
          place_id: string
          user_id: string
          code: string
          used: boolean
          expires_at: string
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          user_id: string
          code: string
          used?: boolean
          expires_at: string
          created_at?: string
        }
        Update: {
          id?: string
          place_id?: string
          user_id?: string
          code?: string
          used?: boolean
          expires_at?: string
          created_at?: string
        }
        Relationships: []
      }
      place_edits: {
        Row: {
          id: string
          place_id: string
          submitted_by: string
          patch: Json
          status: 'pending' | 'approved' | 'rejected'
          created_at: string
        }
        Insert: {
          id?: string
          place_id: string
          submitted_by: string
          patch: Json
          status?: 'pending' | 'approved' | 'rejected'
          created_at?: string
        }
        Update: {
          id?: string
          place_id?: string
          submitted_by?: string
          patch?: Json
          status?: 'pending' | 'approved' | 'rejected'
          created_at?: string
        }
      }
    }
    Views: {
      [_ in never]: never
    }
    Functions: {
      [_ in never]: never
    }
    Enums: {
      [_ in never]: never
    }
  }
}

// Convenience row-type aliases
export type Profile = Database['public']['Tables']['profiles']['Row']
export type Place = Database['public']['Tables']['places']['Row']
export type Confirmation = Database['public']['Tables']['confirmations']['Row']
export type Flag = Database['public']['Tables']['flags']['Row']
export type PlaceProduct = Database['public']['Tables']['place_products']['Row']
export type ProductVote = Database['public']['Tables']['product_votes']['Row']
export type TagSynonym = Database['public']['Tables']['tag_synonyms']['Row']
export type Review = Database['public']['Tables']['reviews']['Row']
export type PlaceOwner = Database['public']['Tables']['place_owners']['Row']
export type OtpToken = Database['public']['Tables']['otp_tokens']['Row']
export type PlaceEdit = Database['public']['Tables']['place_edits']['Row']
