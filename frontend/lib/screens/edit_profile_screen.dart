import 'package:flutter/material.dart';

import '../services/api_client.dart';
import '../services/user_service.dart';
import '../theme/app_color_scheme.dart';

/// "Informations personnelles" — reached from [ProfileScreen]'s account
/// settings section.
///
/// Backed by `PUT /api/users/me` (`UserController.java`) via [UserService].
/// Le numéro de téléphone n'est pas modifiable ici (identifiant de compte,
/// voir `CLAUDE.md` §3) et il n'existe pas encore d'endpoint de changement
/// de mot de passe côté backend — seuls nom complet et email sont éditables.
class EditProfileScreen extends StatefulWidget {
  final String initialFullName;
  final String? initialEmail;
  final String phoneNumber;

  const EditProfileScreen({
    super.key,
    required this.initialFullName,
    required this.initialEmail,
    required this.phoneNumber,
  });

  @override
  State<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends State<EditProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  late final _fullNameController = TextEditingController(text: widget.initialFullName);
  late final _emailController = TextEditingController(text: widget.initialEmail ?? '');
  bool _isSaving = false;

  @override
  void dispose() {
    _fullNameController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isSaving = true);
    try {
      await UserService().updateProfile(
        fullName: _fullNameController.text.trim(),
        email: _emailController.text.trim().isEmpty ? '' : _emailController.text.trim(),
      );
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } on ApiException catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(e.message)));
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = context.colors;

    return Scaffold(
      backgroundColor: colors.homeBackground,
      body: SafeArea(
        child: Column(
          children: [
            Container(
              color: colors.surface,
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
              child: Row(
                children: [
                  IconButton(icon: Icon(Icons.arrow_back_rounded, color: colors.textDark), onPressed: () => Navigator.of(context).pop()),
                  Expanded(
                    child: Text('Informations personnelles', textAlign: TextAlign.center, style: TextStyle(fontWeight: FontWeight.bold, color: colors.textDark)),
                  ),
                  const SizedBox(width: 48),
                ],
              ),
            ),
            Expanded(
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(20),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      TextFormField(
                        controller: _fullNameController,
                        decoration: const InputDecoration(labelText: 'Nom complet'),
                        validator: (v) => (v == null || v.trim().length < 2) ? 'Nom trop court.' : null,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _emailController,
                        decoration: const InputDecoration(labelText: 'Email (optionnel)'),
                        keyboardType: TextInputType.emailAddress,
                        validator: (v) {
                          if (v == null || v.trim().isEmpty) return null;
                          return v.contains('@') ? null : 'Email invalide.';
                        },
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        initialValue: widget.phoneNumber,
                        enabled: false,
                        decoration: const InputDecoration(labelText: 'Téléphone (non modifiable)'),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'Le changement de mot de passe n\'est pas encore disponible depuis l\'app.',
                        style: TextStyle(fontSize: 12, color: colors.textMuted),
                      ),
                      const SizedBox(height: 24),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton(
                          onPressed: _isSaving ? null : _save,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: colors.primary,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 14),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                          ),
                          child: _isSaving
                              ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                              : const Text('Enregistrer', style: TextStyle(fontWeight: FontWeight.w600)),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
