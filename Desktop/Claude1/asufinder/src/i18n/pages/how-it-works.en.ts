export const howItWorksEn = {
  title: 'How ASUFinder works',
  intro:
    'ASUFinder is a community directory for Greater Asunción. Anyone can search, add, and verify businesses. The more people participate, the more reliable it becomes.',

  sections: {
    search: {
      id: 'buscar',
      title: 'Search',
      body: 'Type what you\'re looking for in the search bar: a business name, a product, or a service. For example: "shoe store", "screws", "Stanley thermos". The search finds both place names and products or services they sell. You can also filter by category using the tags at the top.',
    },
    add: {
      id: 'agregar',
      title: 'Add a place',
      body: 'To add a new business you need to register. Registration is free and only requires an email. Once registered, click "Add place" and fill in the form with the name, neighborhood, category and contact details. The place stays in a pending state until it receives 3 confirmations from other users. When approved, it appears in the directory for everyone.',
    },
    confirm: {
      id: 'confirmar',
      title: 'Confirm places',
      body: 'New places need 3 confirmations from different users to appear as verified. When you confirm a place you\'re saying it exists and the information is correct. If you think a place no longer exists or is closed, you can indicate that too. Each confirmation earns +2 points on your profile. Confirming places is the fastest way to earn points and help the community.',
    },
    claim: {
      id: 'reclamar',
      title: 'Claim your business',
      body: 'If you own or manage a listed business, you can claim it. The process uses WhatsApp verification: we send a 6-digit code to the WhatsApp number registered for the place. Once verified, you have owner access: you can edit the place\'s details, add a product and service catalog, and manage reviews.',
    },
    points: {
      id: 'puntos',
      title: 'Points & ranking',
      intro:
        'Every action on ASUFinder adds or subtracts points from your profile. Users with the most points appear in the monthly leaderboard.',
      table: {
        headers: ['Action', 'Points'],
        rows: [
          ['Add a place', '+5'],
          ['Confirm a place', '+2'],
          ['Write a review', '+1'],
          ['Add a product tag', '+1'],
          ['Your place receives a flag', '-3'],
          ['Your place accumulates 5 flags', '-5'],
          ['Tag denied', '-2'],
        ],
      },
      leaderboard:
        'The monthly leaderboard resets on the first day of each month. Accumulated points are never lost, but the leaderboard only reflects activity in the current month.',
    },
    faq: {
      id: 'faq',
      title: 'Frequently asked questions',
      items: [
        {
          q: 'A place no longer exists or is closed?',
          a: 'On the place\'s page you can click "Does not exist or is closed". If multiple users report it, the place goes under review and may be marked as closed.',
        },
        {
          q: 'The phone number is wrong?',
          a: 'If you own the business, claim the place to fix the details. If you\'re not the owner, you can leave a review mentioning the error so the community is aware.',
        },
        {
          q: 'The place already exists in the directory?',
          a: 'When adding a place, the system detects possible duplicates and warns you. If it already exists, you can confirm the existing place instead of creating a new one.',
        },
        {
          q: 'What happens with spam or inappropriate content?',
          a: 'Places with multiple flags are automatically placed under review. The moderation team reviews and may reject or remove content that doesn\'t meet community guidelines.',
        },
      ],
    },
  },
}
