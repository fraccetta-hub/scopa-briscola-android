export const howItWorksEs = {
  title: 'Cómo funciona ASUFinder',
  intro:
    'ASUFinder es un directorio comunitario del Gran Asunción. Cualquiera puede buscar, agregar y verificar negocios. Cuanta más gente participa, más confiable se vuelve.',

  sections: {
    search: {
      id: 'buscar',
      title: 'Buscar',
      body: 'Escribí lo que buscás en la barra de búsqueda: el nombre de un negocio, un producto o un servicio. Por ejemplo: "zapatería", "tornillos", "termos Stanley". El buscador encuentra tanto nombres de lugares como productos y servicios que venden. También podés filtrar por categoría usando las etiquetas de la parte superior.',
    },
    add: {
      id: 'agregar',
      title: 'Agregar un lugar',
      body: 'Para agregar un nuevo negocio necesitás registrarte. El registro es gratis y solo requiere un email. Una vez registrado, hacé clic en "Agregar lugar" y completá el formulario con el nombre, barrio, categoría y datos de contacto. El lugar queda en estado pendiente hasta recibir 3 confirmaciones de otros usuarios. Cuando se aprueba, aparece en el directorio para todos.',
    },
    confirm: {
      id: 'confirmar',
      title: 'Confirmar lugares',
      body: 'Los lugares nuevos necesitan 3 confirmaciones de usuarios distintos para aparecer como verificados. Al confirmar un lugar decís que existe y que la información es correcta. Si creés que el lugar ya no existe o está cerrado, podés indicarlo también. Cada confirmación vale +2 puntos para tu perfil. Confirmar lugares es la manera más rápida de ganar puntos y ayudar a la comunidad.',
    },
    claim: {
      id: 'reclamar',
      title: 'Reclamar tu negocio',
      body: 'Si sos dueño o encargado de un negocio listado, podés reclamarlo como tuyo. El proceso usa verificación por WhatsApp: te enviamos un código de 6 dígitos al número de WhatsApp registrado en el lugar. Una vez verificado, tenés acceso de propietario: podés editar los datos del lugar, agregar el catálogo de productos y servicios, y gestionar las reseñas.',
    },
    points: {
      id: 'puntos',
      title: 'Puntos y ranking',
      intro:
        'Cada acción en ASUFinder suma o resta puntos a tu perfil. Los usuarios con más puntos aparecen en el ranking mensual.',
      table: {
        headers: ['Acción', 'Puntos'],
        rows: [
          ['Agregar un lugar', '+5'],
          ['Confirmar un lugar', '+2'],
          ['Escribir una reseña', '+1'],
          ['Agregar un tag de producto', '+1'],
          ['Tu lugar recibe una denuncia', '-3'],
          ['Tu lugar acumula 5 denuncias', '-5'],
          ['Tag rechazado', '-2'],
        ],
      },
      leaderboard:
        'El ranking mensual se reinicia cada primer día del mes. Los puntos acumulados no se pierden, pero el ranking refleja solo la actividad del mes en curso.',
    },
    faq: {
      id: 'faq',
      title: 'Preguntas frecuentes',
      items: [
        {
          q: '¿Un lugar ya no existe o está cerrado?',
          a: 'En la página del lugar podés hacer clic en "No existe o está cerrado". Si varios usuarios lo reportan, el lugar pasa a revisión y puede ser marcado como cerrado.',
        },
        {
          q: '¿El número de teléfono es incorrecto?',
          a: 'Si sos el dueño del negocio, reclamá el lugar para corregir los datos. Si no sos el dueño, podés dejar una reseña indicando el error para que la comunidad lo sepa.',
        },
        {
          q: '¿El lugar ya existe en el directorio?',
          a: 'Al agregar un lugar, el sistema detecta posibles duplicados y te avisa. Si ya existe, podés confirmar el lugar existente en lugar de crear uno nuevo.',
        },
        {
          q: '¿Qué pasa con el contenido spam o inapropiado?',
          a: 'Los lugares con varias denuncias pasan automáticamente a revisión. El equipo de moderación revisa y puede rechazar o eliminar el contenido que no cumpla con las normas de la comunidad.',
        },
      ],
    },
  },
}
