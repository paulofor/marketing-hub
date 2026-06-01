# Listas de propriedades CSS: estrutura e acabamento

Este arquivo separa propriedades CSS em duas ideias práticas:

- **Estrutura / posição / layout**: controla onde o elemento fica, quanto espaço ocupa e como se organiza na tela.
- **Acabamento / apresentação visual**: controla como o elemento aparece visualmente, como cor, fonte, borda, sombra e efeitos.

> Observação: essa divisão é didática. Algumas propriedades podem afetar tanto a estrutura quanto o visual, como `padding`, `border`, `box-sizing`, `transform` e `visibility`. Em etapas de acabamento que precisam garantir qualidade visual mínima de landing, é permitido combinar propriedades de estrutura e apresentação quando elas forem necessárias para legibilidade, responsividade, CTA real, containers, formulários e imagens controladas.

---

## 1. Propriedades relacionadas à estrutura, posição e layout

Use esta categoria para propriedades que respondem perguntas como:

- Onde o elemento fica?
- Quanto espaço ele ocupa?
- Como ele se alinha?
- Como ele se organiza em relação aos outros elementos?
- Como os elementos filhos são distribuídos?

### Posicionamento

```css
position
top
right
bottom
left
z-index
```

### Exibição e fluxo do layout

```css
display
float
clear
visibility
overflow
overflow-x
overflow-y
```

### Tamanho

```css
width
height
min-width
min-height
max-width
max-height
box-sizing
```

### Espaçamento externo

```css
margin
margin-top
margin-right
margin-bottom
margin-left
```

### Espaçamento interno

```css
padding
padding-top
padding-right
padding-bottom
padding-left
```

### Flexbox

```css
flex
flex-direction
flex-wrap
flex-flow
justify-content
align-items
align-content
align-self
gap
row-gap
column-gap
order
flex-grow
flex-shrink
flex-basis
```

### Grid Layout

```css
grid
grid-template
grid-template-columns
grid-template-rows
grid-template-areas
grid-column
grid-column-start
grid-column-end
grid-row
grid-row-start
grid-row-end
grid-area
justify-items
align-items
place-items
justify-content
align-content
place-content
gap
row-gap
column-gap
```

### Transformações que alteram a posição ou dimensão visual

```css
transform
translate
scale
rotate
transform-origin
```

---

## 2. Propriedades relacionadas ao acabamento e apresentação visual

Use esta categoria para propriedades que respondem perguntas como:

- Qual é a cor?
- Qual é a fonte?
- Tem borda?
- Tem sombra?
- Tem transparência?
- Tem animação ou transição?
- Qual é o estilo visual do elemento?

### Cores e fundo

```css
color
background
background-color
background-image
background-size
background-position
background-repeat
background-attachment
background-clip
background-origin
```

### Tipografia

```css
font
font-family
font-size
font-weight
font-style
font-variant
line-height
letter-spacing
word-spacing
```

### Texto

```css
text-align
text-decoration
text-decoration-line
text-decoration-color
text-decoration-style
text-transform
text-shadow
white-space
```

### Bordas

```css
border
border-width
border-style
border-color
border-top
border-right
border-bottom
border-left
border-radius
```

### Contorno

```css
outline
outline-width
outline-style
outline-color
outline-offset
```

### Sombras e transparência

```css
box-shadow
opacity
```

### Filtros e efeitos visuais

```css
filter
backdrop-filter
mix-blend-mode
isolation
```

### Cursor e aparência nativa

```css
cursor
appearance
caret-color
accent-color
```

### Listas

```css
list-style
list-style-type
list-style-position
list-style-image
```

### Imagens e mídia dentro do elemento

```css
object-fit
object-position
```

### Transições

```css
transition
transition-property
transition-duration
transition-timing-function
transition-delay
```

### Animações

```css
animation
animation-name
animation-duration
animation-timing-function
animation-delay
animation-iteration-count
animation-direction
animation-fill-mode
animation-play-state
```

---

## 3. Propriedades mistas

Algumas propriedades podem ser consideradas tanto estrutura quanto acabamento, dependendo do uso.

| Propriedade | Por que é mista? |
|---|---|
| `padding` | Afeta o espaço interno do elemento e também pode mudar sua aparência visual. |
| `border` | É visual, mas também pode aumentar o tamanho total da caixa, dependendo do `box-sizing`. |
| `box-sizing` | Define como `width`, `height`, `padding` e `border` participam do tamanho total do elemento. |
| `transform` | Pode mudar posição, escala ou rotação visual sem necessariamente alterar o fluxo normal do layout. |
| `visibility` | Controla se o elemento aparece, mas ele ainda pode ocupar espaço no layout. |
| `opacity` | Altera a aparência visual, mas não remove o elemento do layout. |

---

## 4. Resumo rápido

| Categoria | Ideia principal | Exemplos |
|---|---|---|
| Estrutura | Define posição, tamanho, espaçamento e organização. | `display`, `position`, `width`, `height`, `margin`, `padding`, `flex`, `grid` |
| Acabamento | Define aparência visual e efeitos. | `color`, `background`, `font-size`, `border-radius`, `box-shadow`, `opacity`, `animation` |

---

## 5. Referências

- MDN Web Docs — CSS Box Model: https://developer.mozilla.org/pt-BR/docs/Web/CSS/Guides/Box_model/Introduction
- MDN Web Docs — CSS display: https://developer.mozilla.org/pt-BR/docs/Web/CSS/Reference/Properties/display
- MDN Web Docs — CSS position: https://developer.mozilla.org/pt-BR/docs/Web/CSS/Reference/Properties/position
- MDN Web Docs — CSS Flexible Box Layout: https://developer.mozilla.org/pt-BR/docs/Web/CSS/Guides/Flexible_box_layout/Basic_concepts
- MDN Web Docs — CSS Reference Properties: https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties
