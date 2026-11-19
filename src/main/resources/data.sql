-- ============================================================
-- DATA.SQL  —  Semilla inicial idempotente: Freddy Fazbear's Pizza (1987)
-- MS Productos — Pedidos360 FNaF2 Edition
-- Los productos se insertan ÚNICAMENTE si no existen por nombre.
-- Esto previene duplicación en reinicios y preserva el stock modificado.
-- ============================================================

-- ── PIZZAS ──────────────────────────────────────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Pizza Margherita de Freddy', 'La clásica de la casa. Base de tomate, mozzarella fresca y albahaca. El favorito de Freddy Fazbear desde 1987.', 12990, 'PIZZA', '/img/pizza-freddy.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pizza Margherita de Freddy');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Pizza Toy Chica Especial', 'Pizza de pollo BBQ con pimientos morrones, cebolla caramelizada y doble queso. ¡Deliciosa como su sonrisa!', 15500, 'PIZZA', '/img/pizza-toy-chica.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pizza Toy Chica Especial');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Pizza Mangle Supreme', 'Pizza enredada de ingredientes: pepperoni, salchicha, champiñones, aceitunas y jalapeños. Caótica pero irresistible.', 14990, 'PIZZA', '/img/pizza-mangle.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pizza Mangle Supreme');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Pizza Toy Bonnie Vegetariana', 'Pizza 100% vegetal: espinaca, tomate cherry, pimiento verde, champiñones y queso de cabra. ¡Fresquísima!', 13500, 'PIZZA', '/img/pizza-bonnie.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pizza Toy Bonnie Vegetariana');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Pizza Balloon Boy', 'Mini pizza individual con queso extra y aceitunas. ¡Hi! ¡Hello! ¡La más pequeña del menú!', 7990, 'PIZZA', '/img/pizza-bb.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pizza Balloon Boy');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Pizza JJ (Edición Secreta)', 'Pizza surprise solo disponible los viernes. Relleno misterioso cambiante cada semana.', 11990, 'PIZZA', '/img/pizza-jj.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Pizza JJ (Edición Secreta)');

-- ── BEBIDAS ─────────────────────────────────────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Fazbear Cola', 'Refresco exclusivo de la pizzería. Sabor a cereza negra con burbujas intensas. Vaso 500ml.', 3500, 'BEBIDA', '/img/bebida-cola.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Fazbear Cola');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Limonada Toy Freddy', 'Limonada natural con menta y azúcar en el borde. Refrescante como una noche en la pizzería.', 4200, 'BEBIDA', '/img/bebida-limonada.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Limonada Toy Freddy');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Batido Golden Freddy', 'Batido de vainilla dorada con chispas de chocolate. Edición limitada. Puede aparecer o desaparecer.', 5990, 'BEBIDA', '/img/bebida-golden.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Batido Golden Freddy');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Agua Springs (500ml)', 'Agua mineral natural. La opción segura cuando los animatrónicos merodean.', 1990, 'BEBIDA', '/img/bebida-agua.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Agua Springs (500ml)');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Jugo de Mango Funtime', 'Jugo natural de mango con hielo triturado. 350ml. Sin conservantes.', 3990, 'BEBIDA', '/img/bebida-mango.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Jugo de Mango Funtime');

-- ── SOUVENIRS ───────────────────────────────────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Máscara de Freddy (Réplica 1987)', 'Réplica oficial de la máscara de Freddy Fazbear. Material ABS resistente. ¡Perfecta para sobrevivir la noche!', 29990, 'SOUVENIR', '/img/souvenir-mascara-freddy.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Máscara de Freddy (Réplica 1987)');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Globo BB Oficial', 'Globo metálico con la cara de Balloon Boy. Incluye cinta y helio por 3 días. ¡Hi! ¡Hello!', 8500, 'SOUVENIR', '/img/souvenir-globo-bb.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Globo BB Oficial');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Caja de Música de Bonnie', 'Caja de música artesanal que toca el tema de Freddy Fazbear. Mecanismo de cuerda. Edición coleccionista.', 45000, 'SOUVENIR', '/img/souvenir-caja-musica.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Caja de Música de Bonnie');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Sombrero de Freddy', 'Sombrero de copa negro oficial de Freddy Fazbear. Talla única ajustable. Perfecto para cumpleaños.', 18990, 'SOUVENIR', '/img/souvenir-sombrero.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Sombrero de Freddy');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Peluche Mangle (30cm)', 'Peluche oficial de Mangle. Viene parcialmente ensamblado, ¡como ella misma! Relleno hipoalergénico.', 24990, 'SOUVENIR', '/img/souvenir-mangle.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Peluche Mangle (30cm)');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Taza Freddy Fazbear 1987', 'Taza cerámica 350ml con ilustración vintage del elenco original de 1987. Apta para lavavajillas.', 14990, 'SOUVENIR', '/img/souvenir-taza.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Taza Freddy Fazbear 1987');

-- ── ANIMATRÓNICOS (Alquiler para eventos) ───────────────────
INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Toy Freddy — Paquete Cumpleaños', 'Alquiler de Toy Freddy por 2 horas para eventos de cumpleaños. Incluye canción, fotos y interacción. Max 30 niños.', 150000, 'ANIMATRONICO', '/img/anim-toy-freddy.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Toy Freddy — Paquete Cumpleaños');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Toy Chica — Show Musical', 'Presentación musical de Toy Chica por 1 hora. Canciones temáticas de Freddy Fazbear''s Pizza. Max 50 personas.', 120000, 'ANIMATRONICO', '/img/anim-toy-chica.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Toy Chica — Show Musical');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Toy Bonnie — Entretenimiento Infantil', 'Toy Bonnie anima la fiesta por 90 minutos: juegos, música en vivo y concursos. Max 40 niños.', 135000, 'ANIMATRONICO', '/img/anim-toy-bonnie.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Toy Bonnie — Entretenimiento Infantil');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Mangle — Paquete Decorativo', 'Mangle como decoración estática para eventos. Incluye instalación y desmontaje. Duración ilimitada del evento.', 80000, 'ANIMATRONICO', '/img/anim-mangle.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Mangle — Paquete Decorativo');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Balloon Boy — Entrega de Globos', 'BB entrega globos personalizados a los invitados durante 1 hora. Incluye 50 globos metálicos.', 95000, 'ANIMATRONICO', '/img/anim-bb.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Balloon Boy — Entrega de Globos');

INSERT INTO productos (nombre, descripcion, precio, categoria, imagen_url, stock)
SELECT 'Paquete Completo 1987 (Todos los Animatrónicos)', 'El show completo: Toy Freddy, Toy Chica, Toy Bonnie y BB por 3 horas. El paquete estrella de Freddy Fazbear''s Pizza.', 450000, 'ANIMATRONICO', '/img/anim-completo.png', 20
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE nombre = 'Paquete Completo 1987 (Todos los Animatrónicos)');
