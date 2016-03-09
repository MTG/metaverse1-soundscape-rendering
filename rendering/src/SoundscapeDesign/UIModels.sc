MV1_DatabaseType { }
MV1_AnnotatedDatabase : MV1_DatabaseType { }
MV1_SegmentedDatabase : MV1_DatabaseType { }

MV1_Exception : Exception { }
MV1_XMLException : MV1_Exception { }

MV1_XMLParser {
	var <node;
	
	*new { | node |
		^this.newCopyArgs(node)
	}
	
	getChildNodes {
		^Pseq(node.getChildNodes).asStream.collect { |x| this.class.new(x) }
	}
	withChildNodes { |function|
		^function.value(this.getChildNodes)
	}
	getNodeName {
		^node.getNodeName
	}
	getAttribute { |name|
		^node.getAttribute(name)
	}
	getText {
		^node.getText
	}
	getTextOf { | tagName |
		^if (this.getNodeName == tagName) {
			this.getText
		}{
			throw(MV1_XMLException("Expected tag " ++ tagName ++ ", got " ++ this.getNodeName));
		}
	}
	withChildElement { | tagName, ifPresent, ifAbsent |
		var x = node.getChildNodes.detect { | node |  (node.getNodeType == DOMNode.node_ELEMENT) && (tagName == node.getNodeName) };
		^if (x.notNil) {
			ifPresent.value(this.class.new(x))
		}{
			ifAbsent.value
		}
	}
	getChildElement { | tagName |
		^this.withChildElement(tagName, { |x| x }, { throw(MV1_XMLException("Missing element " ++ tagName)) })
	}
	withElement { | tagName, ifPresent, ifAbsent |
		var x = node.getElement(tagName);
		^if (x.notNil) {
			ifPresent.value(this.class.new(x))
		}{
			ifAbsent.value
		}
	}
	getElement { | tagName |
		^this.withElement(tagName, { |x| x }, { throw(MV1_XMLException("Missing element " ++ tagName)) })
	}
	getElementsByTagName { | tagName |
		^node.getElementsByTagName(tagName).collect { |x| this.class.new(x) }
	}
	getSimpleDataOf { | theName |
		var simpleData = node.getElementsByTagName("SimpleData").detect { |x| x.getAttribute("name") == theName };
		^if (simpleData.notNil) {
			simpleData.getText
		}
	}
	select { | function |
		^node.getChildNodes.select { |node| this.class.new(node) }.select(function)
	}
	detect { |function|
		^node.getChildNodes.collect { |node| this.class.new(node) }.detect(function)
	}
	getElementsByTagName0 { | tagName | // non-recursive version
		^node.getChildNodes.select { | node | ((node.getNodeType == DOMNode.node_ELEMENT) && (tagName == node.getNodeName)) }.collect { |node| this.class.new(node) }
	}
}

MV1_SimpleData {
	var <name
	  , <default
	  , <value
	  ;
	
	*new { | name, default |
		^super.new.prInitSimpleData(name, default)
	}

	fromXML { | node |
		var textValue = node.getSimpleDataOf(name);
//		if (textValue.isNil) {
//			this.value_(default);
//		} {
		if (textValue.notNil) {
			this.value_(this.valueFromStringRepresentation(textValue));
		};
	}
	toXML { | node |
/*		if (name == "gain") {
			[name, value, default].postln;
		};
*/
		if (value != default) {
			var elt = node.getOwnerDocument.createElement("SimpleData").setAttribute("name", name);
			var text = node.getOwnerDocument.createTextNode(this.stringValue);
			elt.appendChild(text);
			node.appendChild(elt);
		};
	}

	value_ { | anObject |
		value = anObject;
	}
	stringValue {
		^this.valueToStringRepresentation(value)
	}
	valueFromStringRepresentation { | aString |
		^this.subclassResponsibility(thisMethod)
	}
	valueToStringRepresentation { | anObject |
		^anObject.asString
	}

	printOn { | aStream |
		this.printClassNameOn(aStream);
		aStream << $( << this.stringValue << $);
	}

	prInitSimpleData { | aString, anObject |
		name = aString;
		default = anObject;
		value = default;
	}
}

MV1_SimpleString : MV1_SimpleData {
	valueFromStringRepresentation { | aString |
		^aString
	}
}

MV1_SimpleBoolean : MV1_SimpleData {
	valueFromStringRepresentation { | aString |
		^aString.asInteger.booleanValue
	}
	valueToStringRepresentation { | anObject |
		^anObject.binaryValue.asString
	}
}

MV1_SimpleNumber : MV1_SimpleData {
	var <spec;
	
	*new { | name, default, minVal(-inf), maxVal(inf) |
		^super.new(name, default).prInitSpec(minVal, maxVal)
	}
	value_ { | anObject |
		super.value_(anObject.clip(spec.minval, spec.maxval))
	}
	prInitSpec { | minVal, maxVal |
		spec = ControlSpec(minVal, maxVal, default: default);
	}
}

MV1_SimpleInteger : MV1_SimpleNumber {
	valueFromStringRepresentation { | aString |
		^aString.asInteger
	}
}

MV1_SimpleFloat : MV1_SimpleNumber {
	valueFromStringRepresentation { | aString |
		^aString.asFloat
	}
}

MV1_SimpleRect : MV1_SimpleData {
	valueFromStringRepresentation { | aString |
		var x, y, w, h;
		#x, y, w, h = aString.split($,);
		^Rect((x ? 0).asFloat, (y ? 0).asFloat, (w ? 0).asFloat, (h ? 0).asFloat)
	}
	valueToStringRepresentation { | aRect |
		// Store only top-left coordinate of empty Rect.
		var a = if ((aRect.width == 0) && (aRect.height == 0)) { [aRect.left, aRect.top] } { aRect.asArray };
		^a.join(",")
	}
}

MV1_Model : Model {
	changed { | ... args |
		if (args.isNil || args.isEmpty) {
			super.changed(\changed);
		} {
			super.changed(*args);
		}
	}
	setValue { | data, value, notify=true |
		data.value = value;
		this.changed;
	}
}

MV1_Soundscape : MV1_Model {
	var <node		// XMLNode
	  , name		// String
	  , width		// Float
	  , height	// Float
	  , type		// MV1_DatabaseType
	  , <zones 	// [Placemark]
	  ;
	
	*new {
		^super.new.prInitSoundscape
	}
	*load { | kmlPath, xmlPath |
		var x = this.new
		  , dbPath = if (xmlPath.isNil)
						{ var p = PathName(kmlPath)
						    , q = PathName(p.folderName +/+ p.fileNameWithoutExtension ++ ".xml");
						  if (q.isFile) { q.fullPath } }
						{ xmlPath };
		^x.load(kmlPath, dbPath)
	}
	
	load { | kmlPath, xmlPath |
		var kml = DOMDocument(kmlPath)
		  , db = if (xmlPath.notNil) { MV1_Database.load(xmlPath) } { MV1_Database.new };
		this.prInitFromXML(MV1_XMLParser(kml.getDocumentElement), db);
		this.changed;
	}
	save { | kmlPath, xmlPath |
		this.prSaveToXML;
		node.getOwnerDocument.format;
		File.use(kmlPath, "w", { |io| node.getOwnerDocument.write(io) });
		if (xmlPath.notNil) {
			MV1_Database.fromConcepts(this.name, this.concepts.collect(_.concept)).save(xmlPath);
		};
	}
	
	name {     ^name.value }
	name_ { |x| name.value = x; this.changed }
	width {     ^width.value }
	width_ { |x| width.value = x; this.changed }
	height {     ^height.value }
	height_ { |x| height.value = x; this.changed }
	type {     ^type.value }
	type_ { |x| type.value = x; this.changed }

	concepts {
//		^placemarks.select { |x| x.extendedData.isKindOf(MV1_Concept) }
		^this.zones.collect { |pm| pm.zone.concepts }.flatten
	}
//	zones {
//		^placemarks.select { |x| x.extendedData.isKindOf(MV1_Zone) }
//	}
	placemarks {
		^this.zones ++ this.concepts
	}

	// ** Private **
	prInitSoundscape {
		name = MV1_SimpleString("name", "UNNAMED");
		width = MV1_SimpleFloat("width", 0);
		height = MV1_SimpleFloat("height", 0);
		type = MV1_SimpleString("type", "segmentedDatabase");
		zones = [];
	}
	prInitFromXML { | parser, db |
		var folder = { parser.getElement("Folder") } try: { throw(MV1_XMLException("missing soundscape folder")) };
		var extData = { folder.getChildElement("ExtendedData") }.try;
		var nameNode = folder.getElementsByTagName0("name")[0];
		if (extData.notNil) {
			var nodeParser = extData.getElement("SchemaData");
			if (nodeParser.node.getAttribute("schemaUrl") != "#soundscape_id") {
				throw(MV1_XMLException("expected ExtendedData of type #soundscape_id"));
			};
			node = nodeParser.node;
			name.fromXML(nodeParser);
			width.fromXML(nodeParser);
			height.fromXML(nodeParser);
			type.fromXML(nodeParser);
		}{
			// Add "ExtendedData" node if missing; the data fields will do the rest during saving
			extData = folder.node.getOwnerDocument.createElement("ExtendedData");
			folder.node.appendChild(extData);
			node = extData.getOwnerDocument.createElement("SchemaData");
			extData.appendChild(node);
			if (nameNode.notNil) {
				name.value = nameNode.getText;
			};
		};
		zones = folder.getElementsByTagName0("Folder").collect { |node| this.prNewZone(node, db) }.reject(_.isNil);
//		placemarks = parser.node.select { |x| x.getNodeType == DOMNode.node_ELEMENT && { x.getTagName == "Placemark" } }
//					.collect { |node| MV1_KMLPlacemark.new.initFromXML(MV1_XMLParser(node), db) };
	}
	prNewZone { |folder, db|
		var zoneName = (folder.getElementsByTagName0("name")[0] ?? { throw(MV1_XMLException("missing name for zone folder")) }).getText
		  , zonePM = folder.getElementsByTagName0("Placemark").detect { |node|
					var name = node.getElementsByTagName0("name")[0];
					name.notNil && { name.getText == zoneName } }
		  , concepts = folder.getElementsByTagName0("Placemark").select { |node|
				var name = node.getElementsByTagName0("name")[0];
				name.notNil && { name.getText != zoneName }
			  }.collect { |p|
				MV1_KMLPlacemark.new.initFromXML(p, MV1_Concept(p.getElement("name").getText, p.node).initFromXML(p, db), db)
			  };
		^zonePM !? { MV1_KMLPlacemark.new.initFromXML(zonePM, MV1_Zone(zoneName, concepts, zonePM.node).initFromXML(zonePM, db), db) }
	}
	prSaveToXML {
		var parentNode = node.getParentNode;
		// Eek!
		this.updateConceptPositions;
		this.updateBoundingBox;
		this.updateZoneBoundingBox;
		parentNode.removeChild(node);
		node = node.getOwnerDocument.createElement("SchemaData");
		node.setAttribute("schemaUrl", "#soundscape_id");
		name.toXML(node);
		width.toXML(node);
		height.toXML(node);
		type.toXML(node);
		parentNode.appendChild(node);
		this.placemarks.do(_.prSaveToXML);
	}

	// Latitude/Longitude conversion and bounding box computation
	
	*llDistance { |l1,l2|
		var rr = 6371009; // earth radius
		var dist = rr * sqrt((((l1.x*pi/180) - (l2.x*pi/180)).squared) + (((l1.y*pi/180) - (l2.y*pi/180)).squared));
		// ("Latitude and longitud converted to a two-point distance:" + dist).postln; };
		^dist
	}
	llOrigin {
		^this.concepts.collect { |x| x.position }.inject(inf@inf, { |a,b| (a.x min: b.x )@(a.y min: b.y) });
	}
	updateConceptPositions {
		var ll0 = this.llOrigin;
		var pos, distx, disty;
		this.concepts.do { |pm|
			pos = pm.position;
			distx = this.class.llDistance(ll0, pos.x@ll0.y);
			disty = this.class.llDistance(ll0, ll0.x@pos.y);
			[ll0, pos, distx@disty].postln;
			pm.extendedData.position_(distx@disty);
		}
	}
	conceptBoundingBox {
		^this.class.conceptBoundingBox(this.concepts)
	}
	updateBoundingBox {
		var bb = this.conceptBoundingBox;
		this.width_(bb.width);
		this.height_(bb.height);
	}
	updateZoneBoundingBox {
		this.zones.do { |pm|
			var bb = this.class.conceptBoundingBox(pm.zone.concepts);
			pm.zone.zoneGeometry_(bb);
		};
	}
	*conceptBoundingBox { | concepts |
		^if (concepts.isEmpty) {
			Rect(0, 0, 0, 0)
		} {
			concepts.inject(concepts.first.extendedData.boundingBox, { |a,b| a | b.extendedData.boundingBox })
		}
	}
}

MV1_Zone : MV1_Model {
	var <name					// String
	  , <concepts				// Array
	  , <node
	  , zoneGeometry			// Rect
	  , gain					// Float
	  , virtualUnitMeterRatio	// Float
	  , closeAmbient			// Bool
	  ;
	
	*new { | name, concepts, node |
		^super.new.initName(name, concepts, node)
	}
	initName { | aString, anArray, aNode |
		name = aString;
		concepts = anArray;
		zoneGeometry = MV1_SimpleRect("zoneGeometry");
		gain = MV1_SimpleFloat("gain", 1, 0, inf);
		virtualUnitMeterRatio = MV1_SimpleFloat("virtualUnitMeterRatio", 1, 0, inf);
		closeAmbient = MV1_SimpleBoolean("closeAmbient", false);
		if (aNode.notNil) {
			var extData = aNode.getElement("ExtendedData")
			  , hasSchemaData = false;
			if (extData.notNil) {
				node = extData.getElement("SchemaData");
				hasSchemaData = node.notNil && { node.getAttribute("schemaUrl") == "#zone_id" };
			};
			if (hasSchemaData.not) {
				extData = aNode.getOwnerDocument.createElement("ExtendedData");
				node = aNode.getOwnerDocument.createElement("SchemaData");
				node.setAttribute("schemaUrl", "#zone_id");
				extData.appendChild(node);
				aNode.appendChild(extData);
			};
		};
	}
	initFromXML { | parser, db |
		zoneGeometry.fromXML(parser);
		gain.fromXML(parser);
		virtualUnitMeterRatio.fromXML(parser);
		closeAmbient.fromXML(parser);
	}
	prSaveToXML {
		var parentNode = node.getParentNode;
		parentNode.removeChild(node);
		node = node.getOwnerDocument.createElement("SchemaData");
		node.setAttribute("schemaUrl", "#zone_id");
		zoneGeometry.toXML(node);
		gain.toXML(node);
		virtualUnitMeterRatio.toXML(node);
		closeAmbient.toXML(node);
		parentNode.appendChild(node);
		
	}

	// Model access
	zoneGeometry { ^zoneGeometry.value }
	zoneGeometry_ { | value | this.setValue(zoneGeometry, value) }
	gain { ^gain.value }
	gain_ { | value | this.setValue(gain, value) }
	virtualUnitMeterRatio { ^virtualUnitMeterRatio.value }
	virtualUnitMeterRatio_ { | value | this.setValue(virtualUnitMeterRatio, value) }
	closeAmbient { ^closeAmbient.value }
	closeAmbient_ { | value | this.setValue(closeAmbient, value) }
}

MV1_Concept : MV1_Model {
	classvar <minDistance = 1e-12;
	
	var <name
	  , <node
	  , gain					// Float
	  , conceptGeometry		// Rect
	  , psRandomGeneration		// Rect
	  , continuous			// Bool
	  , multipleGenerativePath	// Int
	  , probability			// Float
	  , ar					// Int
	  , listenedArea			// Float
	  , clone					// ??
	  , <events				// [Event]
	  ;
	
	*new { | name, node |
		^super.new.initName(name, node)
	}
	initName { | aString, aNode |
		name = aString;
		gain = MV1_SimpleFloat("gain", 1, 0, 32);
		conceptGeometry = MV1_SimpleRect("conceptGeometry");
		psRandomGeneration = MV1_SimpleRect("psRandomGeneration");
		continuous = MV1_SimpleBoolean("continuous", false);
		multipleGenerativePath = MV1_SimpleInteger("multipleGenerativePath", 1, 1, 50);
		// It's a probability, but the frequency mapping is more convenient.
		probability = MV1_SimpleFloat("probability", 0, 0, 1).value_(0.5);
		ar = MV1_SimpleInteger("ar", 1, 0, 20);
		listenedArea = MV1_SimpleFloat("listenedArea", 60, 0, 500);
		clone = MV1_SimpleRect("clone");
		events = Set.new;
		if (aNode.notNil) {
			var extData = aNode.getElement("ExtendedData")
			  , hasSchemaData = false;
			if (extData.notNil) {
				node = extData.getElement("SchemaData");
				hasSchemaData = node.notNil && { node.getAttribute("schemaUrl") == "#concept_id" };
			};
			if (hasSchemaData.not) {
				extData = aNode.getOwnerDocument.createElement("ExtendedData");
				node = aNode.getOwnerDocument.createElement("SchemaData");
				node.setAttribute("schemaUrl", "#concept_id");
				extData.appendChild(node);
				aNode.appendChild(extData);
			};
		};
	}
	initFromXML { | parser, db |
		node = parser.node;
		gain.fromXML(parser);
		conceptGeometry.fromXML(parser);
		psRandomGeneration.fromXML(parser);
//		[\initFromXML, conceptGeometry, psRandomGeneration].postln;
		if (conceptGeometry.value.isNil && psRandomGeneration.value.isNil) {
			conceptGeometry.value = Rect(0, 0, 0, 0);
		};
		continuous.fromXML(parser);
		multipleGenerativePath.fromXML(parser);
		probability.fromXML(parser);
		ar.fromXML(parser);
		listenedArea.fromXML(parser);
		clone.fromXML(parser);
		
		// Sanity check
/*		[conceptGeometry, psRandomGeneration, clone].postln;*/
//		if ((conceptGeometry.value.notNil && psRandomGeneration.value.notNil)
//		 || (conceptGeometry.value.isNil && psRandomGeneration.value.isNil && clone.value.isNil)) {
//			throw(MV1_XMLException("Specify exactly one of conceptGeometry or psRandomGeneration or clone"));
//		};
		
		events = Set.newFrom(db.events[this.name] ? []);
	}
	prSaveToXML {
		var parentNode = node.getParentNode;
		parentNode.removeChild(node);
		node = node.getOwnerDocument.createElement("SchemaData");
		node.setAttribute("schemaUrl", "#concept_id");
		conceptGeometry.toXML(node);
		gain.toXML(node);
		psRandomGeneration.toXML(node);
		continuous.toXML(node);
		multipleGenerativePath.toXML(node);
		probability.toXML(node);
		ar.toXML(node);
		listenedArea.toXML(node);
		//clone.toXML(node);
		parentNode.appendChild(node);
	}

	// Model access
	gain { ^gain.value }
	gain_ { | aFloat |
		gain.value = aFloat;
		this.changed;
	}
	conceptGeometry { ^conceptGeometry.value }
	conceptGeometry_ { | aRect |
		conceptGeometry.value = aRect;
		this.changed;
	}
	psRandomGeneration { ^psRandomGeneration.value }
	psRandomGeneration_ { | aRect |
		psRandomGeneration.value = aRect;
		this.changed;
	}
	boundingBox {
		^if (this.conceptGeometry.notNil) {
			this.conceptGeometry
		} {
			if (this.psRandomGeneration.notNil) {
				this.psRandomGeneration
			} {
				Rect(0, 0, 0, 0)
			}
		}
	}
	position_ { | aPoint |
		var rect;
		if (this.conceptGeometry.notNil) {
			rect = this.conceptGeometry;
			this.conceptGeometry_(Rect(aPoint.x, aPoint.y, rect.width, rect.height));
		} {
			if (this.psRandomGeneration.notNil) {
				rect = this.psRandomGeneration;
				this.psRandomGeneration_(Rect(aPoint.x, aPoint.y, rect.width, rect.height));
			}
		}	
	}
	continuous { ^continuous.value }
	continuous_ { | aBool |
		continuous.value = aBool;
		this.changed;
	}

	multipleGenerativePath { ^multipleGenerativePath.value }
	multipleGenerativePath_ { | anInt |
		multipleGenerativePath.value = anInt;
		this.changed;
	}
	probability { ^probability.value }
	probability_ { | aFloat |
		probability.value = aFloat;
		this.changed;
	}
	ar { ^ar.value }
	ar_ { | anInt |
		ar.value = anInt;
		this.changed;
	}
	listenedArea { ^listenedArea.value }
	listenedArea_ { | aFloat |
		listenedArea.value = aFloat;
		this.changed;
	}
	clone { ^clone.value }
	clone_ { | anObject |
		clone.value = anObject;
		this.changed;
	}

	addEvent { | anEvent |
		events.add(anEvent);
		this.changed;
	}
	removeEvent { | anEvent |
		events.remove(anEvent);
		this.changed;
	}
}

MV1_KMLLookAt {
	var <longitude
	  , <latitude
	  , <altitude
	  , <heading
	  , <tilt
	  , <range
	  , <altitudeMode
	  , <gx_altitudeMode
	  ;
	
	initFromXML { | node |
		node.withChildNodes { |s|
			longitude = s.next.getTextOf("longitude").asFloat;
			latitude = s.next.getTextOf("latitude").asFloat;
			altitude = s.next.getTextOf("altitude").asFloat;
			heading = s.next.getTextOf("heading").asFloat;
			tilt = s.next.getTextOf("tilt").asFloat;
			range = s.next.getTextOf("range").asFloat;
			altitudeMode = s.next.getTextOf("altitudeMode");
			gx_altitudeMode = s.next.getTextOf("gx:altitudeMode");
		}
	}
	position { ^latitude@longitude }
}

MV1_KMLPoint {
	var <altitudeMode
	  , <gxAltitudeMode
	  , <coordinates;

	initFromXML { | node |
		node.withChildNodes { |s|
			altitudeMode = s.next.getTextOf("altitudeMode").asSymbol;
			gxAltitudeMode = s.next.getTextOf("gx:altitudeMode").asSymbol;
			coordinates = s.next.getTextOf("coordinates").split($,).collect(_.asFloat);
		}
	}

	position { ^coordinates[1]@coordinates[0] }
}

MV1_KMLPlacemark {
	var <visibility				// Int
	  , <lookAt					// LookAt
	  , <styleUrl				// String
	  , <geometry				// Object
	  , <extendedData			// Object
	  ;
	
	initFromXML { | p, extData, db |
//		name = p.getElement("name").getText;
		visibility = p.withElement("visibility", _.getText, 0);
		lookAt = p.withElement("LookAt", { |x| MV1_KMLLookAt.new.initFromXML(x) });
		styleUrl = p.withElement("styleUrl", _.getText);
		geometry = p.withElement("Point", { |x| MV1_KMLPoint.new.initFromXML(x) });
		extendedData = extData;
		p.withElement("ExtendedData") { |p|
			p.withElement("SchemaData") { |p|
				if (p.node.getAttribute("schemaUrl") == "#concept_id") {
					extendedData.initFromXML(p, db)
			} } };
	}
	name {
		^if (extendedData.notNil) { extendedData.name }
	}
	zone {
		^if (extendedData.isKindOf(MV1_Zone)) {
			extendedData
		} {
			throw(MV1_Exception("Extended data not a Zone"));
		}
	}
	concept {
		^if (extendedData.isKindOf(MV1_Concept)) {
			extendedData
		} {
			throw(MV1_Exception("Extended data not a Concept"));
		}
	}
	position {
		// Return either Point position or LookAt
		^(geometry ? lookAt).position
	}
	prSaveToXML {
		if (extendedData.notNil) { extendedData.prSaveToXML };
	}
}

MV1_Database {
	var <name, <events;

	*new { | name |
		^this.newCopyArgs(name ? "<UNNAMED>", Dictionary.new)
	}
	*load { | path |
		^this.new.prInitFromXMLFile(path)
	}
	*fromConcepts { | aString, anArray |
		^this.new(aString).prInitFromConcepts(anArray)
	}

	save { | path |
		File.use(path, "w", { |io| this.toXML.write(io) });
	}
	toXML {
		var doc = DOMDocument.new
		  , db = doc.createElement("soundscapeDatabase").setAttribute("name", name);
		doc.appendChild(db);
		events.keysValuesDo { | k, v |
			this.prConceptToXML(db, k, v);
		};
		^doc
	}

	== { | other |
		^this.compareObject(other)
	}
	hash {
		^this.instVarSize.collect { |i| this.instVarAt(i) }.hash
	}

	// ** Private **
	prInitFromXMLFile { | path |
		var xml = DOMDocument(path);
		this.prInitFromXML(MV1_XMLParser(xml.getDocumentElement));
	}
	prInitFromXML { | parser |
		var db = parser.getElement("soundscapeDatabase"), assocs;
		name = db.node.getAttribute("name") ?? { throw(MV1_XMLException("Missing attribute 'name'")) };
		assocs = db.getElementsByTagName("soundConcept").collect { |concept|
			var name = concept.getElement("name").getText;
			var events = concept.getElementsByTagName("event").collect { |x| MV1_Event.new.initFromXML(x) }.as(Set);
			name -> events
		};
		events = Dictionary.new.addAll(assocs);
	}
	prInitFromConcepts { | concepts |
		concepts.do { |c| events[c.name] = c.events.as(Set) }
	}
	prConceptToXML { | node, name, events |
		var x = node.getOwnerDocument.createElement("soundConcept")
		  , n = node.getOwnerDocument.createElement("name")
		  , t = node.getOwnerDocument.createTextNode(name);
		n.appendChild(t);
		x.appendChild(n);
		events.do { |e| x.appendChild(e.toXML(x)) };
		node.appendChild(x);
	}
}

MV1_Event : MV1_Model {
	var <url			// URL
	  , <start			// Int
	  , <end			// Int
	  , <sampleRate		// Int
	  , <recDistance	// Float
	  , <normalization	// Float
	  ;

	*new{|url = nil, start = 0, end=0, sampleRate = 44100, recDistance = 0, normalization = 1|
		^super.new.init(url,start,end,sampleRate, recDistance, normalization)
	}
	
	init{|aUrl, aStart, anEnd, aSampleRate, aRecDistance, aNormalization|
		url = aUrl;
		start = aStart;
		end = anEnd;
		sampleRate = aSampleRate;
		recDistance = aRecDistance;
		normalization = aNormalization;
	}
	
	new {
		
	}

	initFromXML { | parser |
		url = parser.node.getAttribute("URL") ?? { throw(MV1_XMLException("Missing attribute URL")) };
		start = (parser.node.getAttribute("start") ? 0).asInteger;
		end = parser.node.getAttribute("end"); if (end.notNil) { end = end.asInteger };
		sampleRate = parser.node.getAttribute("sampleRate"); if (sampleRate.notNil) { sampleRate = sampleRate.asInteger };
		recDistance = (parser.node.getAttribute("recDistance") ? 5).asFloat;
		normalization = (parser.node.getAttribute("normalization") ? 1).asFloat;
	}
	toXML { | node |
		var x = node.getOwnerDocument.createElement("event")
			.setAttribute("URL", url);
		if (start.notNil) {
			x.setAttribute("start", start.asString);
		};
		if (end.notNil) {
			x.setAttribute("end", end.asString);
		};
		if (recDistance.notNil) {
			x.setAttribute("recDistance", recDistance.asString);
		};
		if (normalization.notNil) {
			x.setAttribute("normalization", normalization.asString);
		};
		if (sampleRate.notNil) {
			x.setAttribute("sampleRate", sampleRate.asString);
		};
		^x	
	}
	url_ { | x |
		url = x;
		this.changed;
	}
	start_ { | x |
		start = x;
		this.changed;
	}
	end_ { | x |
		end = x;
		this.changed;
	}
	sampleRate_ { | x |
		sampleRate = x;
		this.changed;
	}
	recDistance_ { | x |
		recDistance = x;
		this.changed;
	}
	normalization_ { | x |
		normalization = x;
		this.changed;
	}
	== { | other |
		^this.compareObject(other)
	}
	hash {
		^this.instVarSize.collect { |i| this.instVarAt(i) }.hash
	}
	printOn { | aStream |
		this.printClassNameOn(aStream);
		aStream << $(;
		[url, start, end, sampleRate, recDistance, normalization].printItemsOn(aStream);
		aStream << $);
	}
}

MV1_TestUIModels : UnitTest {
	// MV1_TestUIModels.run
	test_databaseFromXML {
		MV1_Database.load("/Users/skersten/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/XML_soundscapeDatabase/virtualTravel_example.xml");
		this.assert(true, "should not crash");
	}
	test_soundscapeFromPath {
		var r = MV1_Soundscape.load(
					"/Users/skersten/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/KML_geometry&soundDesign/virtualTravel_example.kml"
					);
		this.assert( r.concepts.every { |p| p.concept.isKindOf(MV1_Concept) });
		this.assert( r.concepts.every { |p| if (p.concept.isNil) { true } { p.concept.events.isEmpty } }
				   , "there should be no events at all" );
	}
	test_soundscapeFromXMLFiles {
		var r = MV1_Soundscape.load(
					"/Users/skersten/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/KML_geometry&soundDesign/virtualTravel_example.kml"
				  , "/Users/skersten/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/XML_soundscapeDatabase/virtualTravel_example.xml"
				  );
		if (r.isException) { r.throw };
		this.assert( r.concepts.any { |p| if (p.concept.isNil) { false } { p.concept.events.notEmpty } }
				   , "there should be at least some events" );
	}
	test_modelChanged {
		var m = MV1_Model.new
		  , x = false;
		SimpleController(m).put(\changed, { x = true; });
		m.changed;
		this.assert(x, "model should have \\changed");
	}
	// Doesn't work because of `node' instance variable.
/*	test_filter {
		var tmpFile = "/tmp/MV1_TestUIModels_test_filter.kml"
		  , x = MV1_Soundscape.load(
					"/Users/sk/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/KML_geometry&soundDesign/virtualTravel_example.kml"
				  ).save(tmpFile)
		  , y = MV1_Soundscape.load(tmpFile);
		this.assert(x.concepts == y.concepts, "save and read unchanged");
	}*/
	test_filterChanged {
		var name = "Wicked Will Wants Wendy Wiener"
		  , width = 512
		  , height = 1024
		  , tmpFile = "/tmp/MV1_TestUIModels_test_filter.kml"
		  , x = MV1_Soundscape.load(
					"/Users/skersten/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/KML_geometry&soundDesign/virtualTravel_example.kml"
				  );
		x.name = name;
		x.width = width;
		x.height = height;
		x.save(tmpFile);
		x = MV1_Soundscape.load(tmpFile);
		this.assert((x.name == name) && (x.width == width) && (x.height == height), "save and read data");
	}
	test_filter_database {
		var tmpFile = "/tmp/MV1_TestUIModels_test_filter_database"
		  , x = MV1_Soundscape.load(
					"/Users/skersten/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/KML_geometry&soundDesign/virtualTravel_example.kml"
				  , "/Users/skersten/projects/mtg/metaverse1/repos/supercollider/src/kml+xml_Schemes/XML_soundscapeDatabase/virtualTravel_example.xml"
				  ).save(tmpFile ++ ".kml", tmpFile ++ ".xml")
		  , y = MV1_Soundscape.load(tmpFile ++ ".kml", tmpFile ++ ".xml");
		this.assert( MV1_Database.fromConcepts(x.name, x.concepts.collect(_.concept))
				     ==
				     MV1_Database.fromConcepts(y.name, y.concepts.collect(_.concept))
			       , "event databases should match before/after");
	}
}
