// ASUFinder — App-wide settings
// Change values here to tune behavior without touching business logic.

export const SETTINGS = {
  // Confirmation system
  confirmations: {
    required: 3,                    // votes to go live
    maxPerUserPerDay: 5,
    minAccountAgeHours: 24,
  },

  // Submission limits
  submissions: {
    maxPlacesPerUserPerDay: 3,
    maxReviewsPerUserPerDay: 10,
    minAccountAgeMinutes: 60,
    maxTagsPerUserPerDay: 10,
    maxNewTagsPerPlacePerWeek: 20,
  },

  // Flagging
  flagging: {
    needsReviewThreshold: 3,       // flags → needs_review status
    closedThreshold: 5,             // flags → closed status
  },

  // Duplicate detection
  duplicates: {
    nameSimilarityThreshold: 0.6,  // pg_trgm similarity score
    barrioSimilarityThreshold: 0.8,
    radiusMeters: 100,
  },

  // WhatsApp OTP
  whatsapp: {
    otpExpiryMinutes: 10,
    otpLength: 6,
  },

  // Points
  points: {
    addPlace: 5,
    confirmPlace: 2,
    writeReview: 1,
    addTag: 1,
    placeGetsFlagged: -3,
    placeGets5Flags: -5,
  },

  // Photo
  photos: {
    maxSizeBytes: 2 * 1024 * 1024, // 2MB
    maxPerPlace: 1,
    allowedTypes: ['image/jpeg', 'image/png', 'image/webp'],
  },

  // Pagination
  pagination: {
    searchResultsPerPage: 20,
    adminListPerPage: 50,
  },

  // Featured listings
  featured: {
    defaultDurationDays: 30,
  },

  // Leaderboard
  leaderboard: {
    topN: 10,
    periodDays: 30,
  },
} as const;
