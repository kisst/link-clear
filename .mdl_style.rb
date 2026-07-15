all

# Prose wraps at 80, but tables and long inline-link rows need headroom.
rule 'MD013', :line_length => 120

# Accept ascending "1. 2. 3." numbering (mdl's default demands all "1.").
rule 'MD029', :style => :ordered
