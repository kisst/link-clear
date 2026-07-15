all

# Prose wraps at 80, but tables, shields.io badges, and long inline-link rows
# are single unwrappable URLs and need headroom.
rule 'MD013', :line_length => 160

# Accept ascending "1. 2. 3." numbering (mdl's default demands all "1.").
rule 'MD029', :style => :ordered
