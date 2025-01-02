INSERT INTO CATEGORIES (id, name) VALUES (11, 'Ciasta');
INSERT INTO CATEGORIES (id, name) VALUES (12, 'Kremy i lukry');
INSERT INTO CATEGORIES (id, name) VALUES (13, 'Drożdżówki');

INSERT INTO LABELS (id, name) VALUES (11, 'Mak');
INSERT INTO LABELS (id, name) VALUES (12, 'Drożdże');
INSERT INTO LABELS (id, name) VALUES (13, 'Świąteczne wypieki');

INSERT INTO RECIPES (id, is_sweet, title, url, owner, last_update, category_id) VALUES (999, true, 'Makownik','https://ilovebake.pl/przepis/makowiec-na-kruchym-spodzie', 'Peppa_Pig', '2024-12-31', 11);

INSERT INTO RECIPES (id, is_sweet, title, url, owner, last_update, category_id) VALUES (1000, false, 'Paszteciki ze szpinakiem','https://kuchnia-domowa-ani.blogspot.com/2018/12/paszteciki-ze-szpinakiem-i-serem.html','Peppa_Pig','2011-11-11', 13);

INSERT INTO RECIPES (id, is_sweet, title, url, owner, last_update, category_id) VALUES (1001, true, 'Lukier cytrynowy', null, 'Peppa_Pig','2024-05-09', 12);

INSERT INTO RECIPES (id, is_sweet, title, url, owner, last_update, category_id) VALUES (1002, true, 'Kremówka','https://www.mysweetworld.pl/2015/11/kremowka-premium/', 'Mummy_Pig', '2000-10-10', 11);

INSERT INTO RECIPE_LABELS (recipe_id, label_id) VALUES (999, 11);
INSERT INTO RECIPE_LABELS (recipe_id, label_id) VALUES (999, 12);
INSERT INTO RECIPE_LABELS (recipe_id, label_id) VALUES (999, 13);
