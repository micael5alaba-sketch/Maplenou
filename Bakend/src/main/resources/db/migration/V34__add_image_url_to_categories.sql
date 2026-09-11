-- Vignette de categorie (URL Cloudinary), meme format que shops.logo_url et product_images.url.
-- NULL = le frontend garde son icone generique en attendant.
ALTER TABLE categories ADD COLUMN image_url VARCHAR(500);
