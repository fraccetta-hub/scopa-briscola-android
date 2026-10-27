// ASUFinder — Place category taxonomy
// OSM import maps to these slugs. Free tags handle the long tail.

export const CATEGORIES = [
  { slug: 'almacen',        label: { es: 'Almacén / Supermercado', en: 'Grocery / Supermarket' }, icon: '🛒', osm: ['supermarket', 'convenience', 'grocery'] },
  { slug: 'ferreteria',     label: { es: 'Ferretería', en: 'Hardware Store' },                    icon: '🔧', osm: ['doityourself', 'hardware'] },
  { slug: 'farmacia',       label: { es: 'Farmacia', en: 'Pharmacy' },                            icon: '💊', osm: ['pharmacy'] },
  { slug: 'nafta',          label: { es: 'Estación de Servicio', en: 'Gas Station' },             icon: '⛽', osm: ['fuel'] },
  { slug: 'carniceria',     label: { es: 'Carnicería', en: 'Butcher' },                           icon: '🥩', osm: ['butcher'] },
  { slug: 'verduleria',     label: { es: 'Verdulería / Frutería', en: 'Greengrocer' },            icon: '🥬', osm: ['greengrocer'] },
  { slug: 'panaderia',      label: { es: 'Panadería', en: 'Bakery' },                             icon: '🍞', osm: ['bakery'] },
  { slug: 'restaurante',    label: { es: 'Restaurante', en: 'Restaurant' },                       icon: '🍽️', osm: ['restaurant'] },
  { slug: 'comida_rapida',  label: { es: 'Snack / Comida Rápida', en: 'Fast Food / Snack' },     icon: '🌮', osm: ['fast_food', 'food_court'] },
  { slug: 'bar',            label: { es: 'Bar / Bebidas', en: 'Bar / Drinks' },                  icon: '🍺', osm: ['bar', 'pub', 'drinks'] },
  { slug: 'taller',         label: { es: 'Taller Mecánico', en: 'Car Repair' },                  icon: '🔩', osm: ['car_repair'] },
  { slug: 'electronica',    label: { es: 'Electrónica', en: 'Electronics' },                     icon: '📱', osm: ['electronics'] },
  { slug: 'ropa',           label: { es: 'Ropa y Calzado', en: 'Clothing & Shoes' },             icon: '👕', osm: ['clothes', 'shoes'] },
  { slug: 'libreria',       label: { es: 'Librería / Papelería', en: 'Bookshop / Stationery' },  icon: '📚', osm: ['books', 'stationery'] },
  { slug: 'peluqueria',     label: { es: 'Peluquería / Barbería', en: 'Hairdresser / Barber' },  icon: '✂️', osm: ['hairdresser', 'barber'] },
  { slug: 'salon_belleza',  label: { es: 'Salón de Belleza', en: 'Beauty Salon' },               icon: '💅', osm: ['beauty'] },
  { slug: 'salud',          label: { es: 'Clínica / Médico', en: 'Clinic / Doctor' },            icon: '🏥', osm: ['clinic', 'doctors', 'hospital'] },
  { slug: 'deportes',       label: { es: 'Deportes', en: 'Sports' },                             icon: '⚽', osm: ['sports'] },
  { slug: 'mascotas',       label: { es: 'Veterinaria / Mascotas', en: 'Vet / Pets' },           icon: '🐾', osm: ['veterinary', 'pet'] },
  { slug: 'tecnologia',     label: { es: 'Tecnología / Informática', en: 'Tech / IT' },          icon: '💻', osm: ['computer'] },
  { slug: 'hogar',          label: { es: 'Hogar y Jardín', en: 'Home & Garden' },               icon: '🏠', osm: ['furniture', 'garden_centre'] },
  { slug: 'educacion',      label: { es: 'Educación / Academia', en: 'Education' },              icon: '🎓', osm: ['school', 'college', 'language_school'] },
  { slug: 'transporte',     label: { es: 'Transporte', en: 'Transport' },                       icon: '🚌', osm: ['taxi', 'bus_station'] },
  { slug: 'servicios',      label: { es: 'Servicios Profesionales', en: 'Professional Services' }, icon: '💼', osm: ['lawyer', 'accountant', 'notary'] },
  { slug: 'otros',          label: { es: 'Otros', en: 'Other' },                                icon: '📌', osm: [] },
] as const;

export type CategorySlug = typeof CATEGORIES[number]['slug'];
