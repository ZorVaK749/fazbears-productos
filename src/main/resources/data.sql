-- ============================================================
-- DATA.SQL  —  Semilla inicial: Freddy Fazbear's Pizza (1987)
-- MS Productos — Pedidos360 FNaF2 Edition
-- ============================================================

-- ── PIZZAS ─────────────────────────────────────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url) VALUES
('Pizza Margherita de Freddy', 'La clásica de la casa. Base de tomate, mozzarella fresca y albahaca. El favorito de Freddy Fazbear desde 1987.', 12.99, 'PIZZA', '/img/pizza-freddy.png'),
('Pizza Toy Chica Especial', 'Pizza de pollo BBQ con pimientos morrones, cebolla caramelizada y doble queso. ¡Deliciosa como su sonrisa!', 15.50, 'PIZZA', '/img/pizza-toy-chica.png'),
('Pizza Mangle Supreme', 'Pizza enredada de ingredientes: pepperoni, salchicha, champiñones, aceitunas y jalapeños. Caótica pero irresistible.', 14.99, 'PIZZA', '/img/pizza-mangle.png'),
('Pizza Toy Bonnie Vegetariana', 'Pizza 100% vegetal: espinaca, tomate cherry, pimiento verde, champiñones y queso de cabra. ¡Fresquísima!', 13.50, 'PIZZA', '/img/pizza-bonnie.png'),
('Pizza Balloon Boy', 'Mini pizza individual con queso extra y aceitunas. ¡Hi! ¡Hello! ¡La más pequeña del menú!', 7.99, 'PIZZA', '/img/pizza-bb.png'),
('Pizza JJ (Edición Secreta)', 'Pizza surprise solo disponible los viernes. Relleno misterioso cambiante cada semana.', 11.99, 'PIZZA', '/img/pizza-jj.png');

-- ── BEBIDAS ────────────────────────────────────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url) VALUES
('Fazbear Cola', 'Refresco exclusivo de la pizzería. Sabor a cereza negra con burbujas intensas. Vaso 500ml.', 3.50, 'BEBIDA', '/img/bebida-cola.png'),
('Limonada Toy Freddy', 'Limonada natural con menta y azúcar en el borde. Refrescante como una noche en la pizzería.', 4.20, 'BEBIDA', '/img/bebida-limonada.png'),
('Batido Golden Freddy', 'Batido de vainilla dorada con chispas de chocolate. Edición limitada. Puede aparecer o desaparecer.', 5.99, 'BEBIDA', '/img/bebida-golden.png'),
('Agua Springs (500ml)', 'Agua mineral natural. La opción segura cuando los animatrónicos merodean.', 1.99, 'BEBIDA', '/img/bebida-agua.png'),
('Jugo de Mango Funtime', 'Jugo natural de mango con hielo triturado. 350ml. Sin conservantes.', 3.99, 'BEBIDA', '/img/bebida-mango.png');

-- ── SOUVENIRS ──────────────────────────────────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url) VALUES
('Máscara de Freddy (Réplica 1987)', 'Réplica oficial de la máscara de Freddy Fazbear. Material ABS resistente. ¡Perfecta para sobrevivir la noche!', 29.99, 'SOUVENIR', '/img/souvenir-mascara-freddy.png'),
('Globo BB Oficial', 'Globo metálico con la cara de Balloon Boy. Incluye cinta y helio por 3 días. ¡Hi! ¡Hello!', 8.50, 'SOUVENIR', '/img/souvenir-globo-bb.png'),
('Caja de Música de Bonnie', 'Caja de música artesanal que toca el tema de Freddy Fazbear. Mecanismo de cuerda. Edición coleccionista.', 45.00, 'SOUVENIR', '/img/souvenir-caja-musica.png'),
('Sombrero de Freddy', 'Sombrero de copa negro oficial de Freddy Fazbear. Talla única ajustable. Perfecto para cumpleaños.', 18.99, 'SOUVENIR', '/img/souvenir-sombrero.png'),
('Peluche Mangle (30cm)', 'Peluche oficial de Mangle. Viene parcialmente ensamblado, ¡como ella misma! Relleno hipoalergénico.', 24.99, 'SOUVENIR', '/img/souvenir-mangle.png'),
('Taza Freddy Fazbear 1987', 'Taza cerámica 350ml con ilustración vintage del elenco original de 1987. Apta para lavavajillas.', 14.99, 'SOUVENIR', '/img/souvenir-taza.png');

-- ── ANIMATRÓNICOS (Alquiler para eventos) ──────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url) VALUES
('Toy Freddy — Paquete Cumpleaños', 'Alquiler de Toy Freddy por 2 horas para eventos de cumpleaños. Incluye canción, fotos y interacción. Max 30 niños.', 150.00, 'ANIMATRONICO', '/img/anim-toy-freddy.png'),
('Toy Chica — Show Musical', 'Presentación musical de Toy Chica por 1 hora. Canciones temáticas de Freddy Fazbear''s Pizza. Max 50 personas.', 120.00, 'ANIMATRONICO', '/img/anim-toy-chica.png'),
('Toy Bonnie — Entretenimiento Infantil', 'Toy Bonnie anima la fiesta por 90 minutos: juegos, música en vivo y concursos. Max 40 niños.', 135.00, 'ANIMATRONICO', '/img/anim-toy-bonnie.png'),
('Mangle — Paquete Decorativo', 'Mangle como decoración estática para eventos. Incluye instalación y desmontaje. Duración ilimitada del evento.', 80.00, 'ANIMATRONICO', '/img/anim-mangle.png'),
('Balloon Boy — Entrega de Globos', 'BB entrega globos personalizados a los invitados durante 1 hora. Incluye 50 globos metálicos.', 95.00, 'ANIMATRONICO', '/img/anim-bb.png'),
('Paquete Completo 1987 (Todos los Animatrónicos)', 'El show completo: Toy Freddy, Toy Chica, Toy Bonnie y BB por 3 horas. El paquete estrella de Freddy Fazbear''s Pizza.', 450.00, 'ANIMATRONICO', '/img/anim-completo.png');
