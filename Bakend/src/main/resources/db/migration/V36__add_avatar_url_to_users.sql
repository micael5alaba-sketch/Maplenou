-- Photo de profil (URL Cloudinary), meme flux d'upload signe que les images produit/categorie.
-- NULL = le frontend affiche l'initiale du nom en attendant.
ALTER TABLE users ADD COLUMN avatar_url VARCHAR(500);
