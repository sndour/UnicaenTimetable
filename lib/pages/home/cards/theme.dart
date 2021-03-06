import 'package:ez_localization/ez_localization.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:unicaen_timetable/model/settings/entries/application/theme.dart';
import 'package:unicaen_timetable/model/settings/settings.dart';
import 'package:unicaen_timetable/pages/home/cards/card.dart';
import 'package:unicaen_timetable/utils/utils.dart';

/// A card that allows to change the app theme.
class ThemeCard extends MaterialCard {
  /// The card id.
  static const String ID = 'current_theme';

  /// Creates a new theme card instance.
  const ThemeCard() : super(cardId: ID);

  @override
  IconData buildIcon(BuildContext context) => _isDarkMode(context) ? Icons.brightness_3 : Icons.wb_sunny;

  @override
  Color buildColor(BuildContext context) => Colors.indigo[400];

  @override
  String buildSubtitle(BuildContext context) {
    String subtitle = context.getString('home.current_theme.' + (_isDarkMode(context) ? 'dark' : 'light'));
    if(context.get<SettingsModel>().themeEntry.value == ThemeMode.system) {
      subtitle += ' (${context.getString('home.current_theme.auto')})';
    }
    subtitle += '.';
    return subtitle;
  }

  @override
  void onTap(BuildContext context) {
    SettingsModel settingsModel = context.get<SettingsModel>();
    BrightnessSettingsEntry themeEntry = settingsModel.themeEntry;
    themeEntry.value = _isDarkMode(context, listen: false) ? ThemeMode.light : ThemeMode.dark;
    themeEntry.flush();
  }

  /// Returns whether the app is in dark mode.
  bool _isDarkMode(BuildContext context, {bool listen = true}) => Provider.of<SettingsModel>(context, listen: listen).resolveTheme(context).brightness == Brightness.dark;
}