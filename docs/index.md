---
title: Home Page
layout: default
order: 0
---

Welcome to the Run Scanner documentation.

{% for cat in site.category-list %}
### {{ cat }}
<ol>
	{% assign sorted_pages = site.pages | sort: "order" %}
	{% for page in sorted_pages %}
		{% for pc in page.categories %}
			{% if pc == cat %}
				<li><a href="{{ page.url }}">{{ page.title }}</a></li>
			{% endif %} <!-- pc == cat -->
		{% endfor %} <!-- page.categories -->
	{% endfor %} <!-- sorted_pages -->
</ol>
{% endfor %} <!-- site.category-list -->
